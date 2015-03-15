The BPI format (standing for Bit-Packed Image) has been designed for mobile devices with limited storage. BPI mainly targets Java mobiles phones (J2ME) and more precisely the storage of the images required by a _midlet_. Image caracteristics are therefore limited (at most 512 pixels wide and a color space limited to 262 144 colors at most) and metadata are reduced to the strict minimum.

Each image is composed of a _raster_ (size of the image and
raw pixel data) which must be decoded thanks to a _color model_. A color model defines the color space (grayscale or colored), whether the image has an alpha channel and eventually a color palette.

Many images can be stored in the same data flow. A color model can be shared by multiple rasters and a raster can be decoded with several color models (e.g. with different palettes). Relationship between color models and rasters is not encoded into the data flow, to save space, but need to be programmed.

A basic converter is available [here](http://bit-packed-image.googlecode.com/files/bpiconverter.zip) along with its [sources](http://bit-packed-image.googlecode.com/files/bpiconverter-src.zip). At the moment, it doesn't allow indexed color model creation (with a palette) or color
model and raster sharing. A decoder is also available for J2ME
virtual machines [here](http://bit-packed-image.googlecode.com/files/bpi-j2me.zip), again along with its [sources](http://bit-packed-image.googlecode.com/files/bpi-j2me-src.zip). These two software are provided under [LGPL](http://www.gnu.org/licenses/lgpl.html) licence.