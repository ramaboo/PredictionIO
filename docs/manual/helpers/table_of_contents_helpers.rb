module TableOfContentsHelpers
  def table_of_contents(resource)
    content = remove_front_matter_data(File.read(resource.source_file))
    extension = File.extname(resource.source_file)[1..-1] # Trim the first dot.

    if extension != 'md'
      # Render other extensions first if they exist.
      template = Tilt[extension].new { content }
      content = template.render(self, resource.data)
    end

    # Now the custom Markdown TOC.
    markdown = Redcarpet::Markdown.new(Redcarpet::Render::HTML_TOC.new(nesting_level: 2))
    output = markdown.render(content)

    content_tag :aside, output, id: 'table-of-contents', role: 'complementary'
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