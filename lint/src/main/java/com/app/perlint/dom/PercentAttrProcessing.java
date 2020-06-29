/*
 * Copyright (C) 2019 Cricin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.app.perlint.dom;

import com.android.SdkConstants;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.xml.Converter;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.XmlName;
import com.intellij.util.xml.reflect.DomExtension;

import org.jetbrains.android.dom.AndroidDomElement;
import org.jetbrains.android.dom.AndroidDomUtil;
import org.jetbrains.android.dom.AttributeProcessingUtil;
import org.jetbrains.android.dom.attrs.AttributeDefinition;
import org.jetbrains.android.dom.attrs.AttributeDefinitions;
import org.jetbrains.android.dom.attrs.StyleableDefinition;
import org.jetbrains.android.dom.attrs.ToolsAttributeUtil;
import org.jetbrains.android.dom.converters.CompositeConverter;
import org.jetbrains.android.dom.converters.ResourceReferenceConverter;
import org.jetbrains.android.dom.layout.DataBindingElement;
import org.jetbrains.android.dom.layout.LayoutElement;
import org.jetbrains.android.dom.manifest.Manifest;
import org.jetbrains.android.facet.AndroidFacet;
import org.jetbrains.android.resourceManagers.LocalResourceManager;
import org.jetbrains.android.resourceManagers.ModuleResourceManagers;
import org.jetbrains.android.resourceManagers.ResourceManager;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Process  attr's to the current dom element
 */
final class PercentAttrProcessing{
    /**
     * If this attr name exists in xml tag, it's value is treated as
     * declare styleables, shape will also register these attributes
     * to current xml tag.
     */
    private static final String EXTRA_STYLEABLE_ATTR_NAME="extraStyleables";

    static void registerPercentAttributes(
            AndroidFacet facet,AndroidDomElement element,AttributeProcessingUtil.AttributeProcessor callback)
    {
        if(!(element instanceof LayoutElement))
            return;
        if(element instanceof DataBindingElement)
            return;
        XmlTag tag=element.getXmlTag();
        //其他的 merge 等标签不需要注册
        if(isInvalidTagName(tag.getName()))
            return;

        List<String> styleableNames=getStyleablesToRegister(tag.getAttributes());
        for(String styleableName: styleableNames){
            if(styleableName!=null&&styleableName.length()>0){
                registerAttributes(facet,element,styleableName,callback);
            }
        }
    }

    /**
     * 获取样式去注册
     *
     * @param attrs
     * @return
     */
    private static List<String> getStyleablesToRegister(XmlAttribute[] attrs){
        List<String> styleableNames=new ArrayList<>(6);
        styleableNames.add("PercentLayout_Layout");
        for(XmlAttribute attr: attrs){
            String attrName=attr.getLocalName();
            String attrValue=attr.getValue();
            if(EXTRA_STYLEABLE_ATTR_NAME.equals(attrName)&&attrValue!=null){
                if(attrValue.indexOf(',')!=-1){
                    styleableNames.addAll(Arrays.asList(attrValue.split(",")));
                } else{
                    styleableNames.add(attrValue);
                }
            }
        }
        return styleableNames;
    }

    private static boolean isInvalidTagName(String tagName){
        return "layout".equals(tagName)||"fragment".equals(tagName)||"include".equals(tagName)||"requestFocus".equals(
                tagName)||"merge".equals(tagName);
    }

    /**
     * 注册到xml的属性中
     *
     * @param facet
     * @param element
     * @param styleableName
     * @param callback
     */
    private static void registerAttributes(/*NotNull*/ AndroidFacet facet,
            /*NotNull*/ DomElement element,
            /*NotNull*/ String styleableName,
            /*NotNull*/ AttributeProcessingUtil.AttributeProcessor callback)
    {

        ResourceManager manager=getAppResourceManager(facet);
        if(manager==null){
            return;
        }

        AttributeDefinitions attrDefs=manager.getAttributeDefinitions();
        if(attrDefs==null){
            return;
        }

        String namespace=getNamespaceUriByResourcePackage(facet,null);
        StyleableDefinition styleable=attrDefs.getStyleableByName(styleableName);
        if(styleable!=null){
            registerStyleableAttributes(element,styleable,namespace,callback);
        }
    }

    private static boolean sModuleResourceManagerExists=true;

    /**
     * 获取APP的原生属性
     *
     * @param facet
     * @return
     */
    private static ResourceManager getAppResourceManager(AndroidFacet facet){
        ResourceManager manager=null;
        if(sModuleResourceManagerExists){
            try{
                manager=ModuleResourceManagers.getInstance(facet)
                                              .getResourceManager(null);
            } catch(NoClassDefFoundError ignore){
                sModuleResourceManagerExists=false;
            }
        }
        if(!sModuleResourceManagerExists){
            manager=LocalResourceManager.getInstance(facet.getModule());
        }
        return manager;
    }

    /**
     * 获取 xml 的http://schemas.android.com/apk/res-auto声明
     *
     * @param facet
     * @param resPackage
     * @return
     */
    private static String getNamespaceUriByResourcePackage(/*NotNull*/ AndroidFacet facet,
            /*Nullable*/ String resPackage)
    {
        if(resPackage==null){
            if(!isAppProject(facet)||facet.requiresAndroidModel()){
                return SdkConstants.AUTO_URI;
            }
            Manifest manifest=facet.getManifest();
            if(manifest!=null){
                String aPackage=manifest.getPackage()
                                        .getValue();
                if(aPackage!=null&&!aPackage.isEmpty()){
                    return SdkConstants.URI_PREFIX+aPackage;
                }
            }
        } else if(resPackage.equals(SdkConstants.ANDROID_NS_NAME)){
            return SdkConstants.ANDROID_URI;
        }
        return null;
    }

    private static Method sIsAppProjectMethod;

    private static boolean isAppProject(AndroidFacet facet){
        if(sIsAppProjectMethod!=null){
            try{
                return (boolean)sIsAppProjectMethod.invoke(facet.getConfiguration(),(Object[])null);
            } catch(Exception ignore){
            }
        }
        try{
            return facet.getConfiguration()
                        .isAppProject();
        } catch(NoSuchMethodError nsme){
            try{
                sIsAppProjectMethod=facet.getClass()
                                         .getDeclaredMethod("isAppProject",(Class<?>[])null);
                return (boolean)sIsAppProjectMethod.invoke(facet,(Object[])null);
            } catch(Exception ignore){
            }
        }
        return false;
    }

    private static void registerStyleableAttributes(
            /*NotNull*/ DomElement element,
            /*NotNull*/ StyleableDefinition styleable,
            /*Nullable*/ String namespaceUri,
            /*NotNull*/ AttributeProcessingUtil.AttributeProcessor callback)
    {
        for(AttributeDefinition attrDef: styleable.getAttributes()){
            registerAttribute(attrDef,styleable.getName(),namespaceUri,element,callback);
        }
    }

    private static void registerAttribute(
            AttributeDefinition attrDef,
            String parentStyleableName,
            String namespaceKey,
            DomElement element,
            AttributeProcessingUtil.AttributeProcessor callback)
    {
        String name=attrDef.getName();
        if(!SdkConstants.ANDROID_URI.equals(namespaceKey)&&name.startsWith(SdkConstants.ANDROID_NS_NAME_PREFIX)){
            name=name.substring(SdkConstants.ANDROID_NS_NAME_PREFIX.length());
            namespaceKey=SdkConstants.ANDROID_URI;
        }

        XmlName xmlName=new XmlName(name,namespaceKey);
        DomExtension extension=callback.processAttribute(xmlName,attrDef,parentStyleableName);
        if(extension!=null){
            Converter converter=AndroidDomUtil.getSpecificConverter(xmlName,element);
//            if("drawableName".equals(name)){
//                converter = DRAWABLE_CLASS_CONVERTER;
//            }
//            if("drawableId".equals(name)){
//                converter = DRAWABLE_ID_CONVERTER;
//            }
            if(converter==null){
                if(SdkConstants.TOOLS_URI.equals(namespaceKey)){
                    converter=ToolsAttributeUtil.getConverter(attrDef);
                } else{
                    converter=AndroidDomUtil.getConverter(attrDef);
                }
            }

            if(converter!=null){
                extension.setConverter(converter,mustBeSoft(converter,attrDef.getFormats()));
            }
        }
    }

    private static boolean mustBeSoft(Converter converter,Collection formats){
        if(!(converter instanceof CompositeConverter)&&!(converter instanceof ResourceReferenceConverter)){
            return formats.size()>1;
        } else{
            return false;
        }
    }

    private PercentAttrProcessing(){
    }
}
