/*
Copyright (C) 2015, Silent Circle, LLC. All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:
    * Any redistribution, use, or modification is done solely for personal
      benefit and not for any commercial purpose or for monetary gain
    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in the
      documentation and/or other materials provided with the distribution.
    * Neither the name Silent Circle nor the
      names of its contributors may be used to endorse or promote products
      derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL SILENT CIRCLE, LLC BE LIABLE FOR ANY
DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package com.silentcircle.silenttext.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ImageView;

import com.silentcircle.silenttext.R;

public class CircularImageView extends ImageView {

	// Border & Selector configuration variables
	private boolean hasBorder;
	private boolean hasSelector;
	private boolean isSelected;
	private int borderWidth;
	private int canvasSize;
	private int selectorStrokeWidth;

	// Objects used for the actual drawing
	private BitmapShader shader;
	private Bitmap image;
	private Paint paint;
	private Paint paintBorder;
	private Paint paintSelectorBorder;
	private ColorFilter selectorFilter;

	public CircularImageView( Context context ) {
		this( context, null );
	}

	public CircularImageView( Context context, AttributeSet attrs ) {
		this( context, attrs, R.attr.circularImageViewStyle );
	}

	public CircularImageView( Context context, AttributeSet attrs, int defStyle ) {
		super( context, attrs, defStyle );
		init( context, attrs, defStyle );
	}

	/**
	 * Adds a dark shadow to this CircularImageView.
	 */
	public void addShadow() {
		setLayerType( LAYER_TYPE_SOFTWARE, paintBorder );
		paintBorder.setShadowLayer( 4.0f, 0.0f, 2.0f, Color.BLACK );
	}

	@Override
	public boolean dispatchTouchEvent( MotionEvent event ) {
		// Check for clickable state and do nothing if disabled
		if( !isClickable() ) {
			isSelected = false;
			return super.onTouchEvent( event );
		}

		// Set selected state based on Motion Event
		switch( event.getAction() ) {
			case MotionEvent.ACTION_DOWN:
				isSelected = true;
				break;
			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_SCROLL:
			case MotionEvent.ACTION_OUTSIDE:
			case MotionEvent.ACTION_CANCEL:
				isSelected = false;
				break;
		}

		// Redraw image and return super type
		this.invalidate();
		return super.dispatchTouchEvent( event );
	}

	/**
	 * Convert a drawable object into a Bitmap
	 * 
	 * @param drawable
	 * @return
	 */
	public Bitmap drawableToBitmap( Drawable drawable ) {
		if( drawable == null ) { // Don't do anything without a proper drawable
			return null;
		} else if( drawable instanceof BitmapDrawable ) { // Use the getBitmap() method instead if
															// BitmapDrawable
			return ( (BitmapDrawable) drawable ).getBitmap();
		}

		// Create Bitmap object out of the drawable
		Bitmap bitmap = Bitmap.createBitmap( drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888 );
		Canvas canvas = new Canvas( bitmap );
		drawable.setBounds( 0, 0, canvas.getWidth(), canvas.getHeight() );
		drawable.draw( canvas );

		return bitmap;
	}

	/**
	 * Initializes paint objects and sets desired attributes.
	 * 
	 * @param context
	 * @param attrs
	 * @param defStyle
	 */
	private void init( Context context, AttributeSet attrs, int defStyle ) {
		// Initialize paint objects
		paint = new Paint();
		paint.setAntiAlias( true );
		paintBorder = new Paint();
		paintBorder.setAntiAlias( true );
		paintSelectorBorder = new Paint();
		paintSelectorBorder.setAntiAlias( true );

		// load the styled attributes and set their properties
		TypedArray attributes = context.obtainStyledAttributes( attrs, R.styleable.CircularImageView, defStyle, 0 );

		// Check if border and/or border is enabled
		hasBorder = attributes.getBoolean( R.styleable.CircularImageView_border, false );
		hasSelector = attributes.getBoolean( R.styleable.CircularImageView_selector, false );

		// Set border properties if enabled
		if( hasBorder ) {
			int defaultBorderSize = (int) ( 2 * context.getResources().getDisplayMetrics().density + 0.5f );
			setBorderWidth( attributes.getDimensionPixelOffset( R.styleable.CircularImageView_border_width, defaultBorderSize ) );
			setBorderColor( attributes.getColor( R.styleable.CircularImageView_border_color, Color.WHITE ) );
		}

		// Set selector properties if enabled
		if( hasSelector ) {
			int defaultSelectorSize = (int) ( 2 * context.getResources().getDisplayMetrics().density + 0.5f );
			setSelectorColor( attributes.getColor( R.styleable.CircularImageView_selector_color, Color.TRANSPARENT ) );
			setSelectorStrokeWidth( attributes.getDimensionPixelOffset( R.styleable.CircularImageView_selector_stroke_width, defaultSelectorSize ) );
			setSelectorStrokeColor( attributes.getColor( R.styleable.CircularImageView_selector_stroke_color, Color.BLUE ) );
		}

		// Add shadow if enabled
		if( attributes.getBoolean( R.styleable.CircularImageView_shadow, false ) ) {
			addShadow();
		}

		// We no longer need our attributes TypedArray, give it back to cache
		attributes.recycle();
	}

	@Override
	public void invalidate() {
		super.invalidate();
		image = drawableToBitmap( getDrawable() );
		if( shader != null || canvasSize > 0 ) {
			refreshBitmapShader();
		}
	}

	@Override
	public void invalidate( int l, int t, int r, int b ) {
		super.invalidate( l, t, r, b );
		image = drawableToBitmap( getDrawable() );
		if( shader != null || canvasSize > 0 ) {
			refreshBitmapShader();
		}
	}

	@Override
	public void invalidate( Rect dirty ) {
		super.invalidate( dirty );
		image = drawableToBitmap( getDrawable() );
		if( shader != null || canvasSize > 0 ) {
			refreshBitmapShader();
		}
	}

	/**
	 * Returns whether or not this view is currently in its selected state.
	 */
	@Override
	public boolean isSelected() {
		return isSelected;
	}

	private int measureHeight( int measureSpecHeight ) {
		int result = 0;
		int specMode = MeasureSpec.getMode( measureSpecHeight );
		int specSize = MeasureSpec.getSize( measureSpecHeight );

		if( specMode == MeasureSpec.EXACTLY ) {
			// We were told how big to be
			result = specSize;
		} else if( specMode == MeasureSpec.AT_MOST ) {
			// The child can be as large as it wants up to the specified size.
			result = specSize;
		} else {
			// Measure the text (beware: ascent is a negative number)
			result = canvasSize;
		}

		return result + 2;
	}

	private int measureWidth( int measureSpec ) {
		int result = 0;
		int specMode = MeasureSpec.getMode( measureSpec );
		int specSize = MeasureSpec.getSize( measureSpec );

		if( specMode == MeasureSpec.EXACTLY ) {
			// The parent has determined an exact size for the child.
			result = specSize;
		} else if( specMode == MeasureSpec.AT_MOST ) {
			// The child can be as large as it wants up to the specified size.
			result = specSize;
		} else {
			// The parent has not imposed any constraint on the child.
			result = canvasSize;
		}

		return result;
	}

	@Override
	public void onDraw( Canvas canvas ) {
		// Don't draw anything without an image
		if( image == null ) {
			return;
		}

		// Nothing to draw (Empty bounds)
		if( image.getHeight() == 0 || image.getWidth() == 0 ) {
			return;
		}

		// Compare canvas sizes
		int oldCanvasSize = canvasSize;

		canvasSize = canvas.getWidth();
		if( canvas.getHeight() < canvasSize ) {
			canvasSize = canvas.getHeight();
		}

		// Reinitialize shader, if necessary
		if( oldCanvasSize != canvasSize ) {
			refreshBitmapShader();
		}

		// Apply shader to paint
		paint.setShader( shader );

		// Keep track of selectorStroke/border width
		int outerWidth = 0;

		// Get the exact X/Y axis of the view
		int center = canvasSize / 2;

		if( hasSelector && isSelected ) { // Draw the selector stroke & apply the selector filter,
											// if applicable
			outerWidth = selectorStrokeWidth;
			center = ( canvasSize - outerWidth * 2 ) / 2;

			paint.setColorFilter( selectorFilter );
			canvas.drawCircle( center + outerWidth, center + outerWidth, ( canvasSize - outerWidth * 2 ) / 2 + outerWidth - 4.0f, paintSelectorBorder );
		} else if( hasBorder ) { // If no selector was drawn, draw a border and clear the filter
									// instead... if enabled
			outerWidth = borderWidth;
			center = ( canvasSize - outerWidth * 2 ) / 2;

			paint.setColorFilter( null );
			canvas.drawCircle( center + outerWidth, center + outerWidth, ( canvasSize - outerWidth * 2 ) / 2 + outerWidth - 4.0f, paintBorder );
		} else {
			paint.setColorFilter( null );
		}

		// Draw the circular image itself
		canvas.drawCircle( center + outerWidth, center + outerWidth, ( canvasSize - outerWidth * 2 ) / 2 - 4.0f, paint );
	}

	@Override
	protected void onMeasure( int widthMeasureSpec, int heightMeasureSpec ) {
		int width = measureWidth( widthMeasureSpec );
		int height = measureHeight( heightMeasureSpec );
		setMeasuredDimension( width, height );
	}

	/**
	 * Reinitializes the shader texture used to fill in the Circle upon drawing.
	 */
	public void refreshBitmapShader() {
		shader = new BitmapShader( Bitmap.createScaledBitmap( image, canvasSize, canvasSize, false ), Shader.TileMode.CLAMP, Shader.TileMode.CLAMP );
	}

	/**
	 * Sets the CircularImageView's basic border color.
	 * 
	 * @param borderColor
	 */
	public void setBorderColor( int borderColor ) {
		if( paintBorder != null ) {
			paintBorder.setColor( borderColor );
		}
		this.invalidate();
	}

	/**
	 * Sets the CircularImageView's border width in pixels.
	 * 
	 * @param borderWidth
	 */
	public void setBorderWidth( int borderWidth ) {
		this.borderWidth = borderWidth;
		requestLayout();
		this.invalidate();
	}

	/**
	 * Sets the color of the selector to be draw over the CircularImageView. Be sure to provide some
	 * opacity.
	 * 
	 * @param selectorColor
	 */
	public void setSelectorColor( int selectorColor ) {
		selectorFilter = new PorterDuffColorFilter( selectorColor, PorterDuff.Mode.SRC_ATOP );
		this.invalidate();
	}

	/**
	 * Sets the stroke color to be drawn around the CircularImageView during click events when the
	 * selector is enabled.
	 * 
	 * @param borderColor
	 */
	public void setSelectorStrokeColor( int selectorStrokeColor ) {
		if( paintSelectorBorder != null ) {
			paintSelectorBorder.setColor( selectorStrokeColor );
		}
		this.invalidate();
	}

	/**
	 * Sets the stroke width to be drawn around the CircularImageView during click events when the
	 * selector is enabled.
	 * 
	 * @param selectorStrokeWidth
	 */
	public void setSelectorStrokeWidth( int selectorStrokeWidth ) {
		this.selectorStrokeWidth = selectorStrokeWidth;
		requestLayout();
		this.invalidate();
	}
}
