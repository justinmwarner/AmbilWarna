AmbilWarna
==========

AmbilWarna color selector with a transparency selector along side the hue selector.  Original code: https://code.google.com/p/yuku-android-util/


Screenshot
==========

![Example Image][1]

Use
==========

Just like the normal library.  The "new color" or returned color from the dialog will have argb, rather than just rgb.

````java
    // initialColor is the initially-selected color to be shown in the rectangle on the left of the arrow.
	// for example, 0xff000000 is black, 0xff0000ff is blue. Please be aware of the initial 0xff which is the alpha.
	AmbilWarnaDialog dialog = new AmbilWarnaDialog(this, initialColor, new OnAmbilWarnaListener() {
			@Override
			public void onOk(AmbilWarnaDialog dialog, int color) {
					// color is the color selected by the user.
			}
					
			@Override
			public void onCancel(AmbilWarnaDialog dialog) {
					// cancel was selected by the user
			}
	});
````

[1]: https://raw.github.com/justinmwarner/AmbilWarna/master/screenshots/screen.png
    