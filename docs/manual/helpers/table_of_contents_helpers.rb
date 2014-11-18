module TableOfContentsHelpers
  def table_of_contents(resource)
    content = File.read(resource.source_file)
    markdown = Redcarpet::Markdown.new(Redcarpet::Render::HTML_TOC.new(nesting_level: 2))
    content_tag :aside, id: 'table-of-contents', role: 'complementary' do
      markdown.render(remove_front_matter_data(content))
    end
  end

  private

  def remove_front_matter_data(content)
    yaml_regex = /\A(---\s*\n.*?\n?)^((---|\.\.\.)\s*$\n?)/m
    if content =~ yaml_regex
      content = content.sub(yaml_regex, '')
    end

    json_regex = /\A(;;;\s*\n.*?\n?)^(;;;\s*$\n?)/m
    if content =~ json_regex
      content = content.sub(json_regex, '')
    end

    content
  end
end