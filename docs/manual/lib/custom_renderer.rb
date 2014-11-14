require 'middleman-core/renderers/redcarpet'

class CustomRenderer < Middleman::Renderers::MiddlemanRedcarpetHTML
  def initialize(options = {})
    defaults = {
      fenced_code_blocks: true,
      with_toc_data: true,
      no_intra_emphasis: true,
      autolink: true,
      strikethrough: true,
      superscript: true,
      highlight: true,
      underline: true,
      tables: true
    }
    super(defaults.merge(options))
  end

  def paragraph(text)
    if text =~ /^(INFO|SUCCESS|WARNING|DANGER|TODO)[.:](.*?)/
      convert_alerts(text)
    else
      %Q(<p>#{text}</p>)
    end
  end

  private
  def convert_alerts(text)
    text.gsub(/^(INFO|SUCCESS|WARNING|DANGER|TODO)[.:](.*?)(\n(?=\n)|\Z)/m) do
      css_class = $1.downcase
      content = $2.strip
      %Q(<div class="alert #{css_class}"><p>#{content}</p></div>)
    end
  end
end