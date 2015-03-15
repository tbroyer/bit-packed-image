# bit-packed-image
_Automatically exported from code.google.com/p/bit-packed-image_

The BPI format (standing for Bit-Packed Image) has been designed for mobile devices with limited storage. BPI mainly targets Java mobiles phones (J2ME) and more precisely the storage of the images required by a midlet. Image caracteristics are therefore limited (at most 512 pixels wide and a color space limited to 262 144 colors at most) and metadata are reduced to the strict minimum.

Each image is composed of a raster (size of the image and raw pixel data) which must be decoded thanks to a color model. A color model defines the color space (grayscale or colored), whether the image has an alpha channel and eventually a color palette.

Many images can be stored in the same data flow. A color model can be shared by multiple rasters and a raster can be decoded with several color models (e.g. with different palettes). Relationship between color models and rasters is not encoded into the data flow, to save space, but need to be programmed.

A basic converter is available. At the moment, it doesn't allow indexed color model creation (with a palette) or color model and raster sharing. A decoder is also available for J2ME virtual machines. These two software are provided under LGPL licence.
