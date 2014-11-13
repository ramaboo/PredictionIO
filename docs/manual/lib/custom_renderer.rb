require 'middleman-core/renderers/redcarpet'

class CustomRenderer < Middleman::Renderers::MiddlemanRedcarpetHTML
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