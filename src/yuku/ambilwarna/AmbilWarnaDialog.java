package yuku.ambilwarna;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import java.util.Arrays;

public class AmbilWarnaDialog {
	public interface OnAmbilWarnaListener {
		void onCancel(AmbilWarnaDialog dialog);
		void onOk(AmbilWarnaDialog dialog, int color);
	}

	private static final String TAG = "AmbilWarnaDialog";

	final AlertDialog dialog;
	final OnAmbilWarnaListener listener;
	final View viewHue;
	final AmbilWarnaKotak viewSatVal;
	final ImageView viewCursor;
	final ImageView viewTransCursor;
	final View viewOldColor;
	final View viewNewColor;
	final ImageView viewTransparent;
	final ImageView viewTarget;
	final ViewGroup viewContainer;
	final float[] currentColorHsv = new float[4];
	Drawable transBG;
	View transparentOverlay;

	/**
	 * create an AmbilWarnaDialog. call this only from OnCreateDialog() or from a background thread.
	 * 
	 * @param context
	 *            current context
	 * @param color
	 *            current color
	 * @param listener
	 *            an OnAmbilWarnaListener, allowing you to get back error or
	 */
	@SuppressLint("NewApi")
	public AmbilWarnaDialog(final Context context, final int color, final OnAmbilWarnaListener listener) {
		this.listener = listener;
		Color.colorToHSV(color, Arrays.copyOfRange(this.currentColorHsv, 1, this.currentColorHsv.length));

		final View view = LayoutInflater.from(context).inflate(R.layout.ambilwarna_dialog, null);

		this.viewHue = view.findViewById(R.id.ambilwarna_viewHue);
		this.viewSatVal = (AmbilWarnaKotak) view.findViewById(R.id.ambilwarna_viewSatBri);
		this.viewCursor = (ImageView) view.findViewById(R.id.ambilwarna_cursor);
		this.viewTransCursor = (ImageView) view.findViewById(R.id.ambilwarna_cursorTransparency);
		this.viewTransparent = (ImageView) view.findViewById(R.id.ambilwarna_viewTransparency);
		this.viewOldColor = view.findViewById(R.id.ambilwarna_warnaLama);
		this.viewNewColor = view.findViewById(R.id.ambilwarna_warnaBaru);
		this.viewTarget = (ImageView) view.findViewById(R.id.ambilwarna_target);
		this.viewContainer = (ViewGroup) view.findViewById(R.id.ambilwarna_viewContainer);
		this.transparentOverlay = view.findViewById(R.id.ambilwarna_overlay);

		this.viewSatVal.setHue(this.getHue());
		this.viewOldColor.setBackgroundColor(color);
		this.viewNewColor.setBackgroundColor(color);

		this.viewHue.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(final View v, final MotionEvent event) {
				if ((event.getAction() == MotionEvent.ACTION_MOVE)
						|| (event.getAction() == MotionEvent.ACTION_DOWN)
						|| (event.getAction() == MotionEvent.ACTION_UP)) {

					float y = event.getY();
					if (y < 0.f) {
						y = 0.f;
					}
					if (y > AmbilWarnaDialog.this.viewHue.getMeasuredHeight())
					{
						y = AmbilWarnaDialog.this.viewHue.getMeasuredHeight() - 0.001f; // to avoid looping from end to start.
					}
					float hue = 360.f - ((360.f / AmbilWarnaDialog.this.viewHue.getMeasuredHeight()) * y);
					if (hue == 360.f) {
						hue = 0.f;
					}

					AmbilWarnaDialog.this.setHue(hue);
					// update view
					AmbilWarnaDialog.this.viewSatVal.setHue(AmbilWarnaDialog.this.getHue());
					AmbilWarnaDialog.this.updateTransparentView();
					AmbilWarnaDialog.this.moveCursor();
					AmbilWarnaDialog.this.viewNewColor.setBackgroundColor(AmbilWarnaDialog.this.getColor());

					return true;
				}
				return false;
			}
		});
		this.viewTransparent.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(final View v, final MotionEvent event) {
				if ((event.getAction() == MotionEvent.ACTION_MOVE)
						|| (event.getAction() == MotionEvent.ACTION_DOWN)
						|| (event.getAction() == MotionEvent.ACTION_UP)) {

					float y = event.getY();
					if (y < 0.f) {
						y = 0.f;
					}
					if (y > AmbilWarnaDialog.this.viewTransparent.getMeasuredHeight())
					{
						y = AmbilWarnaDialog.this.viewTransparent.getMeasuredHeight() - 0.001f; // to avoid looping from end to start.
					}
					float trans = 255.f - ((255.f / AmbilWarnaDialog.this.viewHue.getMeasuredHeight()) * y);
					if (trans == 255.f) {
						trans = 0.f;
					}
					AmbilWarnaDialog.this.setTransparent(trans);

					// update view
					AmbilWarnaDialog.this.moveTransCursor();
					final int col = AmbilWarnaDialog.this.getColor();
					final int c = Color.argb((int) trans, Color.red(col), Color.green(col), Color.blue(col));
					AmbilWarnaDialog.this.viewNewColor.setBackgroundColor(c);
					return true;
				}
				return false;
			}
		});
		this.viewSatVal.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(final View v, final MotionEvent event) {
				if ((event.getAction() == MotionEvent.ACTION_MOVE)
						|| (event.getAction() == MotionEvent.ACTION_DOWN)
						|| (event.getAction() == MotionEvent.ACTION_UP)) {

					float x = event.getX(); // touch event are in dp units.
					float y = event.getY();

					if (x < 0.f) {
						x = 0.f;
					}
					if (x > AmbilWarnaDialog.this.viewSatVal.getMeasuredWidth()) {
						x = AmbilWarnaDialog.this.viewSatVal.getMeasuredWidth();
					}
					if (y < 0.f) {
						y = 0.f;
					}
					if (y > AmbilWarnaDialog.this.viewSatVal.getMeasuredHeight()) {
						y = AmbilWarnaDialog.this.viewSatVal.getMeasuredHeight();
					}

					AmbilWarnaDialog.this.setSat((1.f / AmbilWarnaDialog.this.viewSatVal.getMeasuredWidth()) * x);
					AmbilWarnaDialog.this.setVal(1.f - ((1.f / AmbilWarnaDialog.this.viewSatVal.getMeasuredHeight()) * y));

					// update view
					AmbilWarnaDialog.this.moveTarget();
					AmbilWarnaDialog.this.viewNewColor.setBackgroundColor(AmbilWarnaDialog.this.getColor());

					return true;
				}
				return false;
			}
		});

		this.dialog = new AlertDialog.Builder(context)
		.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(final DialogInterface dialog, final int which) {
				if (AmbilWarnaDialog.this.listener != null) {
					AmbilWarnaDialog.this.listener.onOk(AmbilWarnaDialog.this, AmbilWarnaDialog.this.getColor());
				}
			}
		})
		.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(final DialogInterface dialog, final int which) {
				if (AmbilWarnaDialog.this.listener != null) {
					AmbilWarnaDialog.this.listener.onCancel(AmbilWarnaDialog.this);
				}
			}
		})
		.setOnCancelListener(new OnCancelListener() {
			// if back button is used, call back our listener.
			@Override
			public void onCancel(final DialogInterface paramDialogInterface) {
				if (AmbilWarnaDialog.this.listener != null) {
					AmbilWarnaDialog.this.listener.onCancel(AmbilWarnaDialog.this);
				}

			}
		})
		.create();
		// kill all padding from the dialog window
		this.dialog.setView(view, 0, 0, 0, 0);

		// move cursor & target on first draw
		final ViewTreeObserver vto = view.getViewTreeObserver();
		vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
			@Override
			public void onGlobalLayout() {
				AmbilWarnaDialog.this.moveCursor();
				AmbilWarnaDialog.this.moveTransCursor();
				AmbilWarnaDialog.this.moveTarget();
				AmbilWarnaDialog.this.updateTransparentView();
				view.getViewTreeObserver().removeGlobalOnLayoutListener(this);
			}
		});
	}

	@SuppressLint("NewApi")
	private int getColor() {
		final int hsv = Color.HSVToColor(Arrays.copyOfRange(this.currentColorHsv, 1, this.currentColorHsv.length));
		return Color.argb((int) this.currentColorHsv[0], Color.red(hsv), Color.green(hsv), Color.blue(hsv));
	}

	public AlertDialog getDialog() {
		return this.dialog;
	}

	private float getHue() {
		return this.currentColorHsv[1];
	}

	private float getSat() {
		return this.currentColorHsv[2];
	}
	private float getTrans() {
		return this.currentColorHsv[0];
	}

	private float getVal() {
		return this.currentColorHsv[3];
	}

	protected void moveCursor() {
		float y = this.viewHue.getMeasuredHeight() - ((this.getHue() * this.viewHue.getMeasuredHeight()) / 360.f);
		if (y == this.viewHue.getMeasuredHeight()) {
			y = 0.f;
		}
		final RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) this.viewCursor.getLayoutParams();
		layoutParams.leftMargin = (int) (this.viewHue.getLeft() - Math.floor(this.viewCursor.getMeasuredWidth() / 2) - this.viewContainer.getPaddingLeft());

		layoutParams.topMargin = (int) ((this.viewHue.getTop() + y) - Math.floor(this.viewCursor.getMeasuredHeight() / 2) - this.viewContainer.getPaddingTop());

		this.viewCursor.setLayoutParams(layoutParams);
	}

	protected void moveTarget() {
		final float x = this.getSat() * this.viewSatVal.getMeasuredWidth();
		final float y = (1.f - this.getVal()) * this.viewSatVal.getMeasuredHeight();
		final RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) this.viewTarget.getLayoutParams();
		layoutParams.leftMargin = (int) ((this.viewSatVal.getLeft() + x) - Math.floor(this.viewTarget.getMeasuredWidth() / 2) - this.viewContainer.getPaddingLeft());
		layoutParams.topMargin = (int) ((this.viewSatVal.getTop() + y) - Math.floor(this.viewTarget.getMeasuredHeight() / 2) - this.viewContainer.getPaddingTop());
		this.viewTarget.setLayoutParams(layoutParams);
	}

	protected void moveTransCursor() {
		float y = this.viewTransparent.getMeasuredHeight() - ((this.getTrans() * this.viewTransparent.getMeasuredHeight()) / 255.f);
		if (y == this.viewTransparent.getMeasuredHeight()) {
			y = 0.f;
		}
		Log.d(TAG, y + " ");
		final RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) this.viewTransCursor.getLayoutParams();
		layoutParams.leftMargin = (int) (this.viewTransparent.getLeft() - Math.floor(this.viewTransCursor.getMeasuredWidth() / 2) - this.viewContainer.getPaddingLeft());

		layoutParams.topMargin = (int) ((this.viewTransparent.getTop() + y) - Math.floor(this.viewTransCursor.getMeasuredHeight() / 2) - this.viewContainer.getPaddingTop());

		this.viewTransCursor.setLayoutParams(layoutParams);
	}

	private void setHue(final float hue) {
		this.currentColorHsv[1] = hue;
	}

	private void setSat(final float sat) {
		this.currentColorHsv[2] = sat;
	}

	protected void setTransparent(final float trans) {
		this.currentColorHsv[0] = trans;
	}
	private void setVal(final float val) {
		this.currentColorHsv[3] = val;
	}

	public void show() {
		this.dialog.show();
	}

	@SuppressLint("NewApi")
	private void updateTransparentView(){
		final GradientDrawable gd = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[] {
				Color.HSVToColor(Arrays.copyOfRange(this.currentColorHsv, 1, this.currentColorHsv.length)), Color.TRANSPARENT
		});
		AmbilWarnaDialog.this.transparentOverlay.setBackgroundDrawable(gd);
	}
}
