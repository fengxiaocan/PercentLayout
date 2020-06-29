/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.app.perc;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.view.MarginLayoutParamsCompat;
import androidx.core.view.ViewCompat;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class PercentLayoutHelper{
    private final ViewGroup mHost;

    private static int mWidthScreen;
    private static int mHeightScreen;

    public PercentLayoutHelper(ViewGroup host)
    {
        mHost=host;
        getScreenSize();
    }

    private void getScreenSize()
    {
        WindowManager wm=(WindowManager)mHost.getContext()
                                             .getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics outMetrics=new DisplayMetrics();
        wm.getDefaultDisplay()
          .getMetrics(outMetrics);
        mWidthScreen=outMetrics.widthPixels;
        mHeightScreen=outMetrics.heightPixels;
    }


    public static void fetchWidthAndHeight(ViewGroup.LayoutParams params,TypedArray array,int widthAttr,int heightAttr)
    {
        params.width=array.getLayoutDimension(widthAttr,0);
        params.height=array.getLayoutDimension(heightAttr,0);
    }

    public void adjustChildren(int widthMeasureSpec,int heightMeasureSpec)
    {
        int widthHint=View.MeasureSpec.getSize(widthMeasureSpec);
        int heightHint=View.MeasureSpec.getSize(heightMeasureSpec);


        for(int i=0, N=mHost.getChildCount();i<N;i++){
            View view=mHost.getChildAt(i);
            ViewGroup.LayoutParams params=view.getLayoutParams();


            if(params instanceof PercentLayoutParams){
                PercentLayoutInfo info=((PercentLayoutParams)params).getPercentLayoutInfo();
                if(info!=null){
                    supportTextSize(widthHint,heightHint,view,info);
                    supportPadding(widthHint,heightHint,view,info);
                    supportMinOrMaxDimesion(widthHint,heightHint,view,info);

                    if(params instanceof ViewGroup.MarginLayoutParams){
                        info.fillMarginLayoutParams((ViewGroup.MarginLayoutParams)params,widthHint,heightHint);
                    } else{
                        info.fillLayoutParams(params,widthHint,heightHint);
                    }
                }
            }
        }


    }

    private void supportPadding(int widthHint,int heightHint,View view,PercentLayoutInfo info)
    {
        int left=view.getPaddingLeft(), right=view.getPaddingRight(), top=view.getPaddingTop(), bottom=
                view.getPaddingBottom();
        PercentLayoutInfo.PercentVal percentVal=info.paddingLeftPercent;
        if(percentVal!=null){
            int base=getBaseByModeAndVal(widthHint,heightHint,percentVal.basemode);
            left=(int)(base*percentVal.percent);
        }
        percentVal=info.paddingRightPercent;
        if(percentVal!=null){
            int base=getBaseByModeAndVal(widthHint,heightHint,percentVal.basemode);
            right=(int)(base*percentVal.percent);
        }

        percentVal=info.paddingTopPercent;
        if(percentVal!=null){
            int base=getBaseByModeAndVal(widthHint,heightHint,percentVal.basemode);
            top=(int)(base*percentVal.percent);
        }

        percentVal=info.paddingBottomPercent;
        if(percentVal!=null){
            int base=getBaseByModeAndVal(widthHint,heightHint,percentVal.basemode);
            bottom=(int)(base*percentVal.percent);
        }
        view.setPadding(left,top,right,bottom);


    }

    private void supportMinOrMaxDimesion(int widthHint,int heightHint,View view,PercentLayoutInfo info)
    {
        try{
            Class clazz=view.getClass();
            invokeMethod("setMaxWidth",widthHint,heightHint,view,clazz,info.maxWidthPercent);
            invokeMethod("setMaxHeight",widthHint,heightHint,view,clazz,info.maxHeightPercent);
            invokeMethod("setMinWidth",widthHint,heightHint,view,clazz,info.minWidthPercent);
            invokeMethod("setMinHeight",widthHint,heightHint,view,clazz,info.minHeightPercent);

        } catch(NoSuchMethodException e){
            e.printStackTrace();
        } catch(InvocationTargetException e){
            e.printStackTrace();
        } catch(IllegalAccessException e){
            e.printStackTrace();
        }

    }

    private void invokeMethod(
            String methodName,
            int widthHint,
            int heightHint,
            View view,
            Class clazz,
            PercentLayoutInfo.PercentVal percentVal)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException
    {
        if(percentVal!=null){
            Method setMaxWidthMethod=clazz.getMethod(methodName,int.class);
            setMaxWidthMethod.setAccessible(true);
            int base=getBaseByModeAndVal(widthHint,heightHint,percentVal.basemode);
            setMaxWidthMethod.invoke(view,(int)(base*percentVal.percent));
        }
    }

    private void supportTextSize(int widthHint,int heightHint,View view,PercentLayoutInfo info)
    {
        //textsize percent support

        PercentLayoutInfo.PercentVal textSizePercent=info.textSizePercent;
        if(textSizePercent==null)
            return;

        int base=getBaseByModeAndVal(widthHint,heightHint,textSizePercent.basemode);
        float textSize=(int)(base*textSizePercent.percent);

        //Button 和 EditText 是TextView的子类
        if(view instanceof TextView){
            ((TextView)view).setTextSize(TypedValue.COMPLEX_UNIT_PX,textSize);
        }
    }

    private static int getBaseByModeAndVal(int widthHint,int heightHint,PercentLayoutInfo.BASEMODE basemode)
    {
        switch(basemode){
            case BASE_HEIGHT:
                return heightHint;
            case BASE_WIDTH:
                return widthHint;
            case BASE_SCREEN_WIDTH:
                return mWidthScreen;
            case BASE_SCREEN_HEIGHT:
                return mHeightScreen;
        }
        return 0;
    }


    public static PercentLayoutInfo getPercentLayoutInfo(Context context,AttributeSet attrs)
    {
        PercentLayoutInfo info=null;
        TypedArray array=context.obtainStyledAttributes(attrs,R.styleable.PercentLayout_Layout);

        info=setWidthAndHeightVal(array,info);

        info=setMarginRelatedVal(array,info);

        info=setTextSizeSupportVal(array,info);

        info=setMinMaxWidthHeightRelatedVal(array,info);

        info=setPaddingRelatedVal(array,info);


        array.recycle();

        return info;
    }

    private static PercentLayoutInfo setWidthAndHeightVal(TypedArray array,PercentLayoutInfo info)
    {
        PercentLayoutInfo.PercentVal percentVal=getPercentVal(array,
                                                              R.styleable.PercentLayout_Layout_layout_widthPercent,
                                                              true);

        if(percentVal!=null){
            info=checkForInfoExists(info);
            info.widthPercent=percentVal;
        }
        percentVal=getPercentVal(array,R.styleable.PercentLayout_Layout_layout_heightPercent,false);

        if(percentVal!=null){
            info=checkForInfoExists(info);
            info.heightPercent=percentVal;
        }
        return info;
    }

    private static PercentLayoutInfo setTextSizeSupportVal(TypedArray array,PercentLayoutInfo info)
    {
        //textSizePercent 默认以高度作为基准
        PercentLayoutInfo.PercentVal percentVal=getPercentVal(array,
                                                              R.styleable.PercentLayout_Layout_layout_textSizePercent,
                                                              false);
        if(percentVal!=null){
            info=checkForInfoExists(info);
            info.textSizePercent=percentVal;
        }

        return info;
    }

    private static PercentLayoutInfo setMinMaxWidthHeightRelatedVal(TypedArray array,PercentLayoutInfo info)
    {
        //maxWidth
        PercentLayoutInfo.PercentVal percentVal=getPercentVal(array,
                                                              R.styleable.PercentLayout_Layout_layout_maxWidthPercent,
                                                              true);
        if(percentVal!=null){
            info=checkForInfoExists(info);
            info.maxWidthPercent=percentVal;
        }
        //maxHeight
        percentVal=getPercentVal(array,R.styleable.PercentLayout_Layout_layout_maxHeightPercent,false);
        if(percentVal!=null){
            info=checkForInfoExists(info);
            info.maxHeightPercent=percentVal;
        }
        //minWidth
        percentVal=getPercentVal(array,R.styleable.PercentLayout_Layout_layout_minWidthPercent,true);
        if(percentVal!=null){
            info=checkForInfoExists(info);
            info.minWidthPercent=percentVal;
        }
        //minHeight
        percentVal=getPercentVal(array,R.styleable.PercentLayout_Layout_layout_minHeightPercent,false);
        if(percentVal!=null){
            info=checkForInfoExists(info);
            info.minHeightPercent=percentVal;
        }

        return info;
    }

    private static PercentLayoutInfo setMarginRelatedVal(TypedArray array,PercentLayoutInfo info)
    {
        //默认margin参考宽度
        PercentLayoutInfo.PercentVal percentVal=getPercentVal(array,
                                                              R.styleable.PercentLayout_Layout_layout_marginPercent,
                                                              true);

        if(percentVal!=null){
            info=checkForInfoExists(info);
            info.leftMarginPercent=percentVal;
            info.topMarginPercent=percentVal;
            info.rightMarginPercent=percentVal;
            info.bottomMarginPercent=percentVal;
        }

        percentVal=getPercentVal(array,R.styleable.PercentLayout_Layout_layout_marginHorizontalPercent,true);
        if(percentVal!=null){
            info=checkForInfoExists(info);
            info.leftMarginPercent=percentVal;
            info.rightMarginPercent=percentVal;
        }

        percentVal=getPercentVal(array,R.styleable.PercentLayout_Layout_layout_marginVerticalPercent,true);
        if(percentVal!=null){
            info=checkForInfoExists(info);
            info.topMarginPercent=percentVal;
            info.bottomMarginPercent=percentVal;
        }

        percentVal=getPercentVal(array,R.styleable.PercentLayout_Layout_layout_marginLeftPercent,true);
        if(percentVal!=null){
            info=checkForInfoExists(info);
            info.leftMarginPercent=percentVal;
        }

        percentVal=getPercentVal(array,R.styleable.PercentLayout_Layout_layout_marginTopPercent,false);
        if(percentVal!=null){
            info=checkForInfoExists(info);
            info.topMarginPercent=percentVal;
        }

        percentVal=getPercentVal(array,R.styleable.PercentLayout_Layout_layout_marginRightPercent,true);
        if(percentVal!=null){
            info=checkForInfoExists(info);
            info.rightMarginPercent=percentVal;
        }

        percentVal=getPercentVal(array,R.styleable.PercentLayout_Layout_layout_marginBottomPercent,false);
        if(percentVal!=null){
            info=checkForInfoExists(info);
            info.bottomMarginPercent=percentVal;
        }
        percentVal=getPercentVal(array,R.styleable.PercentLayout_Layout_layout_marginStartPercent,true);
        if(percentVal!=null){
            info=checkForInfoExists(info);
            info.startMarginPercent=percentVal;
        }

        percentVal=getPercentVal(array,R.styleable.PercentLayout_Layout_layout_marginEndPercent,true);
        if(percentVal!=null){
            info=checkForInfoExists(info);
            info.endMarginPercent=percentVal;
        }

        return info;
    }

    private static PercentLayoutInfo setPaddingRelatedVal(TypedArray array,PercentLayoutInfo info)
    {
        //默认padding以宽度为标准
        PercentLayoutInfo.PercentVal percentVal=getPercentVal(array,
                                                              R.styleable.PercentLayout_Layout_layout_paddingPercent,
                                                              true);
        if(percentVal!=null){
            info=checkForInfoExists(info);
            info.paddingLeftPercent=percentVal;
            info.paddingRightPercent=percentVal;
            info.paddingBottomPercent=percentVal;
            info.paddingTopPercent=percentVal;
        }


        percentVal=getPercentVal(array,R.styleable.PercentLayout_Layout_layout_paddingHorizontalPercent,true);
        if(percentVal!=null){
            info=checkForInfoExists(info);
            info.paddingLeftPercent=percentVal;
            info.paddingRightPercent=percentVal;
        }

        percentVal=getPercentVal(array,R.styleable.PercentLayout_Layout_layout_paddingVerticalPercent,true);
        if(percentVal!=null){
            info=checkForInfoExists(info);
            info.paddingTopPercent=percentVal;
            info.paddingBottomPercent=percentVal;
        }

        percentVal=getPercentVal(array,R.styleable.PercentLayout_Layout_layout_paddingLeftPercent,true);
        if(percentVal!=null){
            info=checkForInfoExists(info);
            info.paddingLeftPercent=percentVal;
        }

        percentVal=getPercentVal(array,R.styleable.PercentLayout_Layout_layout_paddingRightPercent,true);
        if(percentVal!=null){
            info=checkForInfoExists(info);
            info.paddingRightPercent=percentVal;
        }

        percentVal=getPercentVal(array,R.styleable.PercentLayout_Layout_layout_paddingTopPercent,true);
        if(percentVal!=null){
            info=checkForInfoExists(info);
            info.paddingTopPercent=percentVal;
        }

        percentVal=getPercentVal(array,R.styleable.PercentLayout_Layout_layout_paddingBottomPercent,true);
        if(percentVal!=null){
            info=checkForInfoExists(info);
            info.paddingBottomPercent=percentVal;
        }

        return info;
    }

    private static PercentLayoutInfo.PercentVal getPercentVal(TypedArray array,int index,boolean baseWidth)
    {
        String sizeStr=array.getString(index);
        PercentLayoutInfo.PercentVal percentVal=getPercentVal(sizeStr,baseWidth);
        return percentVal;
    }


    @NonNull
    private static PercentLayoutInfo checkForInfoExists(PercentLayoutInfo info)
    {
        info=info!=null ? info : new PercentLayoutInfo();
        return info;
    }


    private static final String REGEX_PERCENT="^(([0-9]+)([.]([0-9]+))?|([.]([0-9]+))?)%([s]?[wh]?)$";

    private static PercentLayoutInfo.PercentVal getPercentVal(String percentStr,boolean isOnWidth)
    {
        //valid param
        if(percentStr==null){
            return null;
        }
        Pattern p=Pattern.compile(REGEX_PERCENT);
        Matcher matcher=p.matcher(percentStr);
        if(!matcher.matches()){
            throw new RuntimeException("the value of layout_xxxPercent invalid! ==>"+percentStr);
        }
//        int len=percentStr.length();
        //extract the float value
        String floatVal=matcher.group(1);
//        String lastAlpha=percentStr.substring(len-1);

        float percent=Float.parseFloat(floatVal)/100f;

        PercentLayoutInfo.PercentVal percentVal=new PercentLayoutInfo.PercentVal();
        percentVal.percent=percent;
        if(percentStr.endsWith(PercentLayoutInfo.BASEMODE.SW)){
            percentVal.basemode=PercentLayoutInfo.BASEMODE.BASE_SCREEN_WIDTH;
        } else if(percentStr.endsWith(PercentLayoutInfo.BASEMODE.SH)){
            percentVal.basemode=PercentLayoutInfo.BASEMODE.BASE_SCREEN_HEIGHT;
        } else if(percentStr.endsWith(PercentLayoutInfo.BASEMODE.PERCENT)){
            if(isOnWidth){
                percentVal.basemode=PercentLayoutInfo.BASEMODE.BASE_WIDTH;
            } else{
                percentVal.basemode=PercentLayoutInfo.BASEMODE.BASE_HEIGHT;
            }
        } else if(percentStr.endsWith(PercentLayoutInfo.BASEMODE.W)){
            percentVal.basemode=PercentLayoutInfo.BASEMODE.BASE_WIDTH;
        } else if(percentStr.endsWith(PercentLayoutInfo.BASEMODE.H)){
            percentVal.basemode=PercentLayoutInfo.BASEMODE.BASE_HEIGHT;
        } else{
            throw new IllegalArgumentException("the "+percentStr+" must be endWith [%|w|h|sw|sh]");
        }

        return percentVal;
    }

    public void restoreOriginalParams()
    {
        for(int i=0, N=mHost.getChildCount();i<N;i++){
            View view=mHost.getChildAt(i);
            ViewGroup.LayoutParams params=view.getLayoutParams();
            if(params instanceof PercentLayoutParams){
                PercentLayoutInfo info=((PercentLayoutParams)params).getPercentLayoutInfo();
                if(info!=null){
                    if(params instanceof ViewGroup.MarginLayoutParams){
                        info.restoreMarginLayoutParams((ViewGroup.MarginLayoutParams)params);
                    } else{
                        info.restoreLayoutParams(params);
                    }
                }
            }
        }
    }

    public boolean handleMeasuredStateTooSmall()
    {
        boolean needsSecondMeasure=false;
        for(int i=0, N=mHost.getChildCount();i<N;i++){
            View view=mHost.getChildAt(i);
            ViewGroup.LayoutParams params=view.getLayoutParams();
            if(params instanceof PercentLayoutParams){
                PercentLayoutInfo info=((PercentLayoutParams)params).getPercentLayoutInfo();
                if(info!=null){
                    if(shouldHandleMeasuredWidthTooSmall(view,info)){
                        needsSecondMeasure=true;
                        params.width=ViewGroup.LayoutParams.WRAP_CONTENT;
                    }
                    if(shouldHandleMeasuredHeightTooSmall(view,info)){
                        needsSecondMeasure=true;
                        params.height=ViewGroup.LayoutParams.WRAP_CONTENT;
                    }
                }
            }
        }
        return needsSecondMeasure;
    }

    private static boolean shouldHandleMeasuredWidthTooSmall(View view,PercentLayoutInfo info)
    {
        int state=ViewCompat.getMeasuredWidthAndState(view)&ViewCompat.MEASURED_STATE_MASK;
        if(info==null||info.widthPercent==null){
            return false;
        }
        return state==ViewCompat.MEASURED_STATE_TOO_SMALL&&info.widthPercent.percent >= 0&&
               info.mPreservedParams.width==ViewGroup.LayoutParams.WRAP_CONTENT;
    }

    private static boolean shouldHandleMeasuredHeightTooSmall(View view,PercentLayoutInfo info)
    {
        int state=ViewCompat.getMeasuredHeightAndState(view)&ViewCompat.MEASURED_STATE_MASK;
        if(info==null||info.heightPercent==null){
            return false;
        }
        return state==ViewCompat.MEASURED_STATE_TOO_SMALL&&info.heightPercent.percent >= 0&&
               info.mPreservedParams.height==ViewGroup.LayoutParams.WRAP_CONTENT;
    }


    public static class PercentLayoutInfo{

        private enum BASEMODE{

            BASE_WIDTH,
            BASE_HEIGHT,
            BASE_SCREEN_WIDTH,
            BASE_SCREEN_HEIGHT;

            public static final String PERCENT="%";
            public static final String W="w";
            public static final String H="h";
            public static final String SW="sw";
            public static final String SH="sh";
        }

        public static class PercentVal{

            public float percent=-1;
            public BASEMODE basemode;

            public PercentVal()
            {
            }

            public PercentVal(float percent,BASEMODE baseMode)
            {
                this.percent=percent;
                this.basemode=baseMode;
            }

            @Override
            public String toString()
            {
                return "PercentVal{"+"percent="+percent+", basemode="+basemode.name()+'}';
            }
        }

        public PercentVal widthPercent;
        public PercentVal heightPercent;

        public PercentVal leftMarginPercent;
        public PercentVal topMarginPercent;
        public PercentVal rightMarginPercent;
        public PercentVal bottomMarginPercent;
        public PercentVal startMarginPercent;
        public PercentVal endMarginPercent;

        public PercentVal textSizePercent;

        //1.0.4 those attr for some views' setMax/min Height/Width method
        public PercentVal maxWidthPercent;
        public PercentVal maxHeightPercent;
        public PercentVal minWidthPercent;
        public PercentVal minHeightPercent;

        //1.0.6 add padding supprot
        public PercentVal paddingLeftPercent;
        public PercentVal paddingRightPercent;
        public PercentVal paddingTopPercent;
        public PercentVal paddingBottomPercent;


        /* package */ final ViewGroup.MarginLayoutParams mPreservedParams;


        public PercentLayoutInfo()
        {
            mPreservedParams=new ViewGroup.MarginLayoutParams(0,0);
        }

        public void fillLayoutParams(ViewGroup.LayoutParams params,int widthHint,int heightHint)
        {
            // Preserve the original layout params, so we can restore them after the measure step.
            mPreservedParams.width=params.width;
            mPreservedParams.height=params.height;

            if(widthPercent!=null){
                int base=getBaseByModeAndVal(widthHint,heightHint,widthPercent.basemode);
                params.width=(int)(base*widthPercent.percent);
            }
            if(heightPercent!=null){
                int base=getBaseByModeAndVal(widthHint,heightHint,heightPercent.basemode);
                params.height=(int)(base*heightPercent.percent);
            }

        }

        public void fillMarginLayoutParams(ViewGroup.MarginLayoutParams params,int widthHint,int heightHint)
        {
            fillLayoutParams(params,widthHint,heightHint);

            // Preserver the original margins, so we can restore them after the measure step.
            mPreservedParams.leftMargin=params.leftMargin;
            mPreservedParams.topMargin=params.topMargin;
            mPreservedParams.rightMargin=params.rightMargin;
            mPreservedParams.bottomMargin=params.bottomMargin;
            MarginLayoutParamsCompat.setMarginStart(mPreservedParams,MarginLayoutParamsCompat.getMarginStart(params));
            MarginLayoutParamsCompat.setMarginEnd(mPreservedParams,MarginLayoutParamsCompat.getMarginEnd(params));

            if(leftMarginPercent!=null){
                int base=getBaseByModeAndVal(widthHint,heightHint,leftMarginPercent.basemode);
                params.leftMargin=(int)(base*leftMarginPercent.percent);
            }
            if(topMarginPercent!=null){
                int base=getBaseByModeAndVal(widthHint,heightHint,topMarginPercent.basemode);
                params.topMargin=(int)(base*topMarginPercent.percent);
            }
            if(rightMarginPercent!=null){
                int base=getBaseByModeAndVal(widthHint,heightHint,rightMarginPercent.basemode);
                params.rightMargin=(int)(base*rightMarginPercent.percent);
            }
            if(bottomMarginPercent!=null){
                int base=getBaseByModeAndVal(widthHint,heightHint,bottomMarginPercent.basemode);
                params.bottomMargin=(int)(base*bottomMarginPercent.percent);
            }
            if(startMarginPercent!=null){
                int base=getBaseByModeAndVal(widthHint,heightHint,startMarginPercent.basemode);
                MarginLayoutParamsCompat.setMarginStart(params,(int)(base*startMarginPercent.percent));
            }
            if(endMarginPercent!=null){
                int base=getBaseByModeAndVal(widthHint,heightHint,endMarginPercent.basemode);
                MarginLayoutParamsCompat.setMarginEnd(params,(int)(base*endMarginPercent.percent));
            }
        }

        @Override
        public String toString()
        {
            return "PercentLayoutInfo{"+"widthPercent="+widthPercent+", heightPercent="+heightPercent+
                   ", leftMarginPercent="+leftMarginPercent+", topMarginPercent="+topMarginPercent+
                   ", rightMarginPercent="+rightMarginPercent+", bottomMarginPercent="+bottomMarginPercent+
                   ", startMarginPercent="+startMarginPercent+", endMarginPercent="+endMarginPercent+
                   ", textSizePercent="+textSizePercent+", maxWidthPercent="+maxWidthPercent+", maxHeightPercent="+
                   maxHeightPercent+", minWidthPercent="+minWidthPercent+", minHeightPercent="+minHeightPercent+
                   ", paddingLeftPercent="+paddingLeftPercent+", paddingRightPercent="+paddingRightPercent+
                   ", paddingTopPercent="+paddingTopPercent+", paddingBottomPercent="+paddingBottomPercent+
                   ", mPreservedParams="+mPreservedParams+'}';
        }

        public void restoreMarginLayoutParams(ViewGroup.MarginLayoutParams params)
        {
            restoreLayoutParams(params);
            params.leftMargin=mPreservedParams.leftMargin;
            params.topMargin=mPreservedParams.topMargin;
            params.rightMargin=mPreservedParams.rightMargin;
            params.bottomMargin=mPreservedParams.bottomMargin;
            MarginLayoutParamsCompat.setMarginStart(params,MarginLayoutParamsCompat.getMarginStart(mPreservedParams));
            MarginLayoutParamsCompat.setMarginEnd(params,MarginLayoutParamsCompat.getMarginEnd(mPreservedParams));
        }

        public void restoreLayoutParams(ViewGroup.LayoutParams params)
        {
            params.width=mPreservedParams.width;
            params.height=mPreservedParams.height;
        }
    }

    public interface PercentLayoutParams{
        PercentLayoutInfo getPercentLayoutInfo();
    }
}
