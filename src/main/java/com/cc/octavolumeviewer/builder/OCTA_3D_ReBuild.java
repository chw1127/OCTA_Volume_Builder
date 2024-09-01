/*
 * To the extent possible under law, the ImageJ developers have waived
 * all copyright and related or neighboring rights to this tutorial code.
 *
 * See the CC0 1.0 Universal license for details:
 *     http://creativecommons.org/publicdomain/zero/1.0/
 */

package com.cc.octavolumeviewer.builder;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.NewImage;
import ij.gui.Overlay;
import ij.plugin.*;
import ij.process.ImageProcessor;
import ij.process.StackConverter;
import ij.process.StackProcessor;

/**
 * A template for processing each pixel of either
 * GRAY8, GRAY16, GRAY32 or COLOR_RGB images.
 *
 * @author Johannes Schindelin
 */
public class OCTA_3D_ReBuild{

	public interface PressListener{
		public void onPress(int value , String msg);
	}

	PressListener pressListener;

	public void setPressListener(PressListener pressListener) {
		this.pressListener = pressListener;
	}
	public void setProgress(int value, String msg) {
		if (pressListener != null) {
			pressListener.onPress(value, msg);
		}
	}
	public ImagePlus process(ImagePlus image) {
		setProgress(10,"加载图像");
//		image.show();
		setProgress(20,"直方图均衡");
		// 原图直方图均衡化
		ImagePlus imageEqualize =  EqualizeHistogram(image);
//		imageEqualize.setTitle("直方图均衡化");
//		imageEqualize.show();

		setProgress(30,"初步深度增强");
		ImagePlus untitled = NewImage.createImage("untitled", image.getWidth(), image.getHeight(), 1, 8, 1);

		// 合同通道
		ImagePlus[] imagePlusArr=new ImagePlus[]{untitled,imageEqualize,untitled,null,null,null,null};
		ImagePlus imageMerged =mergeChannels(imagePlusArr);
		imageMerged.setOpenAsHyperStack(false);  // 这一步很关键，否则下一步深度放大会有问题
//		imageMerged.setTitle("合并通道");
//		imageMerged.show();


		setProgress(40,"水平插值放大");
		ImagePlus newimg=imageMerged.duplicate(); // 克隆一个新图

		// 宽高增大3倍
		ImageProcessor ip = newimg.getProcessor();
		ip.setInterpolationMethod(ImageProcessor.BILINEAR);
		StackProcessor sp = new StackProcessor(newimg.getStack(),ip);
		ImageStack s2 = sp.resize(newimg.getWidth()*3, newimg.getHeight()*3, true);
		newimg.setStack("宽高放大3倍",s2);
		newimg.setOverlay((Overlay)null);
//		newimg.show();
		setProgress(50,"深度插值增强");
		// 深度放到到18
		ImageProcessor ip2 = newimg.getProcessor();
		ip2.setInterpolationMethod(ImageProcessor.BILINEAR);
		ImagePlus imageZscale = new Resizer().zScale(newimg,18,17);
//		imageZscale.setTitle("深度增厚到18");
//		imageZscale.show();
		setProgress(60,"图像RGB通道合并");
		// stack to RGB
		MyRGBStackConverter.convertToRGB(imageZscale);
		setProgress(70,"转灰度，继续增强深度");
		// to 8 bit图
		new StackConverter(imageZscale).convertToGray8();
		// 打卡预览
//		IJ.runPlugIn("fiji.plugin.volumeviewer.Volume_Viewer", "angle_x=0 angle_y=0 angle_z=0 snapshot=1");
//		IJ.runMacro("Volume Viewer","snapshot=1");
//		IJ.run("OCTA Volume Viewer","display_mode=4 scale=0.54 " +
//				"width=1280 height=900 "+
//				"angle_x=0 angle_y=0 angle_z=0 " +
//				"bg_r=0 bg_g=0 bg_b=0 " +
//				"lightRed=255  lightGreen=255 lightBlue=255 " +
//				"useLight=1 " +
//				"shineValue=0 " +  // 0-200
//				"ambientValue=0 " +// 0-1
//				"diffuseValue=1 " +// 0-1
//				"specularValue=0 " +// 0-1
//				"objectLightValue=0 " + // 0-2
//				"snapshot=0");

		return imageZscale;

	}



	public ImagePlus EqualizeHistogram(ImagePlus image){
		ImagePlus newImg=image.duplicate(); // 克隆一个新图
		int stackSize = image.getStackSize();
		ContrastEnhancer contrastEnhancer=new ContrastEnhancer();
		contrastEnhancer.equalize(newImg);
		newImg.setTitle("直方图均衡化后");
		return newImg;
	}



	static ImagePlus mergeChannels(ImagePlus[] images){
		int maxChannels=7;
		boolean createComposite=true;

		ImageStack[] stacks = new ImageStack[maxChannels];
		for (int i=0; i<maxChannels; i++)
			stacks[i] = images[i]!=null?images[i].getStack():null;
		ImagePlus imp2;
		boolean fourOrMoreChannelRGB = false;
		for (int i=3; i<maxChannels; i++) {
			if (stacks[i]!=null) {
				if (!createComposite)
					fourOrMoreChannelRGB=true;
				createComposite = true;
			}
		}

		imp2 = new RGBStackMerge().mergeHyperstacks(images, false);

		for (int i=0; i<images.length; i++) {
			if (images[i]!=null) {
				imp2.setCalibration(images[i].getCalibration());
				break;
			}
		}
		if (fourOrMoreChannelRGB) {
			if (imp2.getNSlices()==1&&imp2.getNFrames()==1) {
				imp2 = imp2.flatten();
				imp2.setTitle("RGB");
			}
		}

		return imp2;

	}

	void error(String msg) {
		IJ.error("OCTA_3D_ReBuild", msg);
	}


	/**
	 * Main method for debugging.
	 *
	 * For debugging, it is convenient to have a method that starts ImageJ, loads
	 * an image and calls the plugin, e.g. after setting breakpoints.
	 *
	 * @param args unused
	 */
	public static void main(String[] args) throws Exception {
		// set the plugins.dir property to make the plugin appear in the Plugins menu
		// see: https://stackoverflow.com/a/7060464/1207769
		Class<?> clazz = OCTA_3D_ReBuild.class;
		java.net.URL url = clazz.getProtectionDomain().getCodeSource().getLocation();
		java.io.File file = new java.io.File(url.toURI());
		System.setProperty("plugins.dir", file.getAbsolutePath());
		//配置系统的插件路径，可以把所有的插件都装上
		System.setProperty("plugins.dir", "D:\\Program Files\\Fiji.app-2023\\plugins");
		System.out.println(System.getProperty("plugins.dir"));

		// start ImageJ
//		new ImageJ();

		// open the Clown sample
		ImagePlus image = IJ.openImage("d:/1.png");
		image.show();
		System.out.println(clazz.getName());
		// run the plugin
		IJ.runPlugIn(clazz.getName(), "");
	}
}
