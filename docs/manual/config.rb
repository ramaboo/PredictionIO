require 'lib/custom_renderer'

# General Settings
set :css_dir,       'stylesheets'
set :js_dir,        'javascripts'
set :images_dir,    'images'
set :partials_dir,  'partials'
set :trailing_slash, false

activate :directory_indexes
activate :gzip
activate :syntax, line_numbers: true

# Markdown
set :markdown_engine, :redcarpet
set :markdown,
  renderer: ::CustomRenderer,
  fenced_code_blocks: true,
  with_toc_data: true,
  no_intra_emphasis: true,
  autolink: true,
  strikethrough: true,
  superscript: true,
  highlight: true,
  underline: true,
  tables: true



# Sprockets
sprockets.append_path File.join root, 'bower_components'
sprockets.append_path File.join root, 'vendor/assets'


# Development Settings
configure :development do
  set :scheme, 'http'
  set :host, Middleman::PreviewServer.host
  set :port, Middleman::PreviewServer.port
  Slim::Engine.set_default_options pretty: true, sort_attrs: false
  set :debug_assets, true
end

# Build Settings
configure :build do
  set :scheme, 'http'
  set :host, 'docs.prediction.io'
  set :port, 80
  Slim::Engine.set_default_options pretty: false, sort_attrs: false
  activate :asset_hash
  activate :minify_css
  activate :minify_javascript
  activate :minify_html do |html|
    html.remove_multi_spaces        = true
    html.remove_comments            = true
    html.remove_intertag_spaces     = false
    html.remove_quotes              = false
    html.simple_doctype             = false
    html.remove_script_attributes   = true
    html.remove_style_attributes    = false
    html.remove_link_attributes     = false
    html.remove_form_attributes     = false
    html.remove_input_attributes    = false
    html.remove_javascript_protocol = true
    html.remove_http_protocol       = false
    html.remove_https_protocol      = false
    html.preserve_line_breaks       = false
    html.simple_boolean_attributes  = false
  end
end

# Hacks
Slim::Engine.disable_option_validator! # https://github.com/middleman/middleman/issues/612
