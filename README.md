application-loadtest
====================

XWiki Load Test Application created to generate content in wiki in a way which is realistic.

Uses pages taken from xwiki.org to provide content:

* Platform subwiki (Features, AdminGuide, DevGuide)
* Enterprise subwiki (Getting Started)
* Dev subwiki (Community, Design)

Characteristics:
* Patched images with added space reference so they can be easily used in other pages and still work
* It includes the image macro, which is used to insert images on xwiki.org
* removed pages which contain redirects, they mess up the generated pages

* Bundles 3 AWM applications (Small, Medium, Large) which will be populated with generated entries
* Script to create usernames as similar as the way a regular user creates them using the UI
