/** Copyright 2014 TappingStone, Inc.
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  *     http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */

package io.prediction.data.storage.hbase

import io.prediction.data.storage.Event
import io.prediction.data.storage.EventValidation
import io.prediction.data.storage.Events
import io.prediction.data.storage.EventJson4sSupport
import io.prediction.data.storage.DataMap
import io.prediction.data.storage.StorageError
import io.prediction.data.storage.hbase.HBEventsUtil.RowKey
import io.prediction.data.storage.hbase.HBEventsUtil.RowKeyException
import io.prediction.data.storage.hbase.HBEventsUtil.PartialRowKey

import grizzled.slf4j.Logging

import org.json4s.DefaultFormats
import org.json4s.JObject
import org.json4s.native.Serialization.{ read, write }

import org.joda.time.DateTime
import org.joda.time.DateTimeZone

import org.apache.hadoop.hbase.NamespaceDescriptor
import org.apache.hadoop.hbase.NamespaceExistException
import org.apache.hadoop.hbase.HTableDescriptor
import org.apache.hadoop.hbase.HColumnDescriptor
import org.apache.hadoop.hbase.TableName
import org.apache.hadoop.hbase.client.HTable
import org.apache.hadoop.hbase.client.Put
import org.apache.hadoop.hbase.client.Get
import org.apache.hadoop.hbase.client.Delete
import org.apache.hadoop.hbase.client.Result
import org.apache.hadoop.hbase.client.Scan
import org.apache.hadoop.hbase.util.Bytes
import org.apache.hadoop.hbase.filter.FilterList
import org.apache.hadoop.hbase.filter.RegexStringComparator
import org.apache.hadoop.hbase.filter.SingleColumnValueFilter
import org.apache.hadoop.hbase.filter.CompareFilter.CompareOp

import scala.collection.JavaConversions._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext

class HBEvents(val client: HBClient, val namespace: String)
  extends Events with Logging {

  implicit val formats = DefaultFormats + new EventJson4sSupport.DBSerializer

  def resultToEvent(result: Result, appId: Int): Event =
    HBEventsUtil.resultToEvent(result, appId)

  def getTable(appId: Int) = client.connection.getTable(
    HBEventsUtil.tableName(namespace, appId))

  override
  def init(appId: Int): Boolean = {
    // check namespace exist
    val existingNamespace = client.admin.listNamespaceDescriptors()
      .map(_.getName)
    if (!existingNamespace.contains(namespace)) {
      val nameDesc = NamespaceDescriptor.create(namespace).build()
      info(s"The namespace ${namespace} doesn't exist yet. Creating now...")
      client.admin.createNamespace(nameDesc)
    }

    val tableName = TableName.valueOf(HBEventsUtil.tableName(namespace, appId))
    if (!client.admin.tableExists(tableName)) {
      info(s"The table ${tableName.getNameAsString()} doesn't exist yet." +
        " Creating now...")
      val tableDesc = new HTableDescriptor(tableName)
      tableDesc.addFamily(new HColumnDescriptor("e"))
      tableDesc.addFamily(new HColumnDescriptor("r")) // reserved
      client.admin.createTable(tableDesc)
    }
    true
  }

  override
  def remove(appId: Int): Boolean = {
    val tableName = TableName.valueOf(HBEventsUtil.tableName(namespace, appId))
    client.admin.disableTable(tableName)
    client.admin.deleteTable(tableName)
    true
  }

  override
  def close() = {
    client.admin.close()
    client.connection.close()
  }

  override
  def futureInsert(event: Event, appId: Int)(implicit ec: ExecutionContext):
    Future[Either[StorageError, String]] = {
    Future {
      val table = getTable(appId)
      val (put, rowKey) = HBEventsUtil.eventToPut(event, appId)
      table.put(put)
      table.flushCommits()
      table.close()
      Right(rowKey.toString)
    }/*.recover {
      case e: Exception => Left(StorageError(e.toString))
    }*/
  }



  override
  def futureGet(eventId: String, appId: Int)(implicit ec: ExecutionContext):
    Future[Either[StorageError, Option[Event]]] = {
      Future {
        val table = getTable(appId)
        val rowKey = RowKey(eventId)
        val get = new Get(rowKey.toBytes)

        val result = table.get(get)
        table.close()

        if (!result.isEmpty()) {
          val event = resultToEvent(result, appId)
          Right(Some(event))
        } else {
          Right(None)
        }
      }.recover {
        case e: RowKeyException => Left(StorageError(e.toString))
        case e: Exception => throw e
      }
    }

  override
  def futureDelete(eventId: String, appId: Int)(implicit ec: ExecutionContext):
    Future[Either[StorageError, Boolean]] = {
    Future {
      val table = getTable(appId)
      val rowKey = RowKey(eventId)
      val exists = table.exists(new Get(rowKey.toBytes))
      table.delete(new Delete(rowKey.toBytes))
      table.close()
      Right(exists)
    }
  }

  override
  def futureGetByAppId(appId: Int)(implicit ec: ExecutionContext):
    Future[Either[StorageError, Iterator[Event]]] = {
      futureGetGeneral(
        appId = appId,
        startTime = None,
        untilTime = None,
        entityType = None,
        entityId = None,
        limit = None,
        reversed = None)
    }

  override
  def futureGetByAppIdAndTime(appId: Int, startTime: Option[DateTime],
    untilTime: Option[DateTime])(implicit ec: ExecutionContext):
    Future[Either[StorageError, Iterator[Event]]] = {
      futureGetGeneral(
        appId = appId,
        startTime = startTime,
        untilTime = untilTime,
        entityType = None,
        entityId = None,
        limit = None,
        reversed = None)
  }

  override
  def futureGetByAppIdAndTimeAndEntity(appId: Int,
    startTime: Option[DateTime],
    untilTime: Option[DateTime],
    entityType: Option[String],
    entityId: Option[String])(implicit ec: ExecutionContext):
    Future[Either[StorageError, Iterator[Event]]] = {
      futureGetGeneral(
        appId = appId,
        startTime = startTime,
        untilTime = untilTime,
        entityType = entityType,
        entityId = entityId,
        limit = None,
        reversed = None)
  }

  override
  def futureGetGeneral(
    appId: Int,
    startTime: Option[DateTime],
    untilTime: Option[DateTime],
    entityType: Option[String],
    entityId: Option[String],
    limit: Option[Int],
    reversed: Option[Boolean] = Some(false))(implicit ec: ExecutionContext):
    Future[Either[StorageError, Iterator[Event]]] = {
      Future {
        val table = getTable(appId)

        val scan = HBEventsUtil.createScan(
          startTime = startTime,
          untilTime = untilTime,
          entityType = entityType,
          entityId = entityId,
          reversed = reversed)
        val scanner = table.getScanner(scan)
        table.close()

        val eventsIter = scanner.iterator()

        val results: Iterator[Result]  = (
          if (limit.isEmpty) eventsIter
          else eventsIter.take(limit.get)
        )

        val events = results.map { resultToEvent(_, appId) }

        Right(events)
      }
  }

  override
  def futureDeleteByAppId(appId: Int)(implicit ec: ExecutionContext):
    Future[Either[StorageError, Unit]] = {
    Future {
      // TODO: better way to handle range delete
      val table = getTable(appId)
      val scan = new Scan()
      val scanner = table.getScanner(scan)
      val it = scanner.iterator()
      while (it.hasNext()) {
        val result = it.next()
        table.delete(new Delete(result.getRow()))
      }
      scanner.close()
      table.close()
      Right(())
    }
  }

}
