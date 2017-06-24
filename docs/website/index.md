---
layout: longpage
title: Website Developer Documentation
permalink: /dev_docs/website_dev/
---

## Website source code links
- [triplea-game.org - Github.io jekyll+markdown](https://github.com/triplea-game/triplea-game.github.io)
- [tripleawarclub.org - Warclub apache+PHP](https://github.com/triplea-game/tripleawarclub.org)


## Getting Started (github.io website)
The TripleA site runs on [Jekyll](http://jekyllrb.com/) and [GitHub Pages](https://pages.github.com/). Pages are written in [markdown](https://github.com/adam-p/markdown-here/wiki/Markdown-Cheatsheet).

This is a guide for contribution to the website. For now, please start all feature requests as issues. Please note that we will often hesitate to accept pull requests without prior approval, but it is very easy to start an issue first, so that we can help you fit your idea into the project.

## About the site
This is the official TripleA website, which is in early development. Please  start an issue if you want to help out/have a request. There are a few places where you can go to discuss the site:

* [Development Forum Thread for General Discussion About the Site on the Dev Forum](http://tripleadev.1671093.n2.nabble.com/TripleA-Website-td7589306.html#a7589426)
* [Development Forum Thread for volunteering to help out](http://tripleadev.1671093.n2.nabble.com/TripleA-Website-Development-td7589352.html#a7589383)
* [Trello Board for Content Changes](https://trello.com/b/Q5ndwlqD)

## I have an idea for / want to write a page, what should I do?
1. Comment on the [README card](https://trello.com/c/3R0yIP99) on the [Trello board](https://trello.com/b/Q5ndwlqD), and a dev should respond by either addressing your concern or making a card for it.
2. Write any content in [Markdown](https://daringfireball.net/projects/markdown/) with the [YAML front matter](http://jekyllrb.com/docs/frontmatter/) like the other pages. If you can, follow the instructions below to test your changes.
3. If you have confirmation, then send a pull request, which will will first test, then if everything works alright, merge. If you're not sure on how to use Git, [look no further](https://github.com/triplea-game/triplea/wiki/DevSetup--Git-Workflow).

## Making code changes

If you are interested in making changes to the design, functionality, or structure of the blog, you're going to need to set up a local environment and understand some of the technologies behind the site.

**1. Install Jekyll and dependencies.** First thing you're going to need to do is set up Jekyll and the appropriate dependencies so you can develop locally. The easiest way to install everything is to use [RubyGems](https://rubygems.org/pages/download) and follow the instructions on the [Jekyll](http://jekyllrb.com/docs/installation/) website. In addition to the basic Jekyll installation, you also need to install a few dependency gems. After installing RubyGems on your machine, you really only need to run one basic command:

`gem install jekyll`

**2. Run a local version of the blog.** Using Git, clone the latest version of this repository to your local machine using the following command:

`git clone https://github.com/triplea-game/triplea-game.github.io.git`

Then go into root of the folder and run `jekyll serve`:

`cd triplea-game.github.io && jekyll serve`

This will get a local version of the blog running on your machine, accessible on `localhost:4000, or whatever the terminal tells you`

**3. Read up on the documentation.** To really understand how to develop the site there are a few things you're going to need to read up on to make meaningful changes:

- [Jekyll](http://jekyllrb.com/docs/home/) The static site generator used for the site
- [GitHub Pages](https://help.github.com/articles/using-jekyll-with-pages/) How the site is hosted
- [Liquid](https://help.shopify.com/themes/liquid/basics) The templating language used with Jekyll by Shopify

## Points of Maintenance

Though the site has been designed to allow as few points of maintenance as possible, here are areas that must be updated.

### Every Time TripleA is Updated

* The version number and links round line 11 of `_config.yml` need to be updated with information about the new release. A pull request can be made, and merged as soon as the new version is live.
* (Deprecated) Update `news.md` with a link and a changelog.
