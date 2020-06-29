# PercentLayout
百分比布局库,可以在xml中自动提示attr属性
<a href="https://github.com/hongyangAndroid/android-percent-support-extend">根据hongyang的库做的扩展</a>

* 不改变原有库的用法
* 添加了PercentLinearLayout
* 支持宽度高度百分比设置值
* 支持margin百分比设置值
* 支持padding百分比设置值
* 支持minWidth , maxWidth , minHeight , maxHeight百分比设置值
* 支持TextView、Button、EditText的textSize
* 支持ScrollView内部为Linearlayout
* 支持任何View的尺寸参照屏幕的宽或者高

###支持的属性 :

* layout_heightPercent
* layout_widthPercent
* layout_marginBottomPercent
* layout_marginEndPercent
* layout_marginLeftPercent
* layout_marginPercent
* layout_marginRightPercent
* layout_marginStartPercent
* layout_marginTopPercent
* layout_textSizePercent
* layout_maxWidthPercent
* layout_maxHeightPercent
* layout_minWidthPercent
* layout_minHeightPercent
* layout_paddingPercent
* layout_paddingTopPercent
* layout_paddingBottomPercent
* layout_paddingLeftPercent
* layout_paddingRightPercent
* layout_aspectRatio 绝对的宽高比例 值可以取 10%w , 10%h , 10%

对于值可以取：10%w , 10%h , 10% , 10%sw , 10%sh


***layout_aspectRatio的说明***
   
    10% == 10%w : 以宽度为基准,把高度固定为宽度的 10%
    10%h : 以高度为基准,把宽度固定为高度的 10%