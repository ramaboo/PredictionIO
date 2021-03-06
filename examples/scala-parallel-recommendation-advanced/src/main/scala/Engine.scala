package org.examples.recommendation

import io.prediction.controller._

case class Query(
  val user: Int,
  val num: Int
) extends Serializable

case class ProductScore(
  product: Int,
  score: Double
) extends Serializable

case class PredictedResult(
  val productScores: Array[ProductScore]
) extends Serializable

// RecommendationEngine using file as DataSource
object RecommendationEngineWithFile extends IEngineFactory {
  def apply() = {
    new Engine(
      classOf[FileDataSource],
      classOf[Preparator],
      Map("als" -> classOf[ALSAlgorithm]),
      classOf[Serving])
  }
}

// RecommendationEngine using Mongo as DataSource
object RecommendationEngineWithMongo extends IEngineFactory {
  def apply() = {
    new Engine(
      classOf[MongoDataSource],
      classOf[Preparator],
      Map("als" -> classOf[ALSAlgorithm]),
      classOf[Serving])
  }
}
