package com.buihha.audiorecorder.bean;

import android.text.TextUtils;

import com.buihha.audiorecorder.data.manager.AppInitManager;
import com.buihha.audiorecorder.utils.CollectionUtil;
import com.buihha.audiorecorder.utils.Logger;
import com.buihha.audiorecorder.utils.ValidateUtil;
import com.zjb.rxokhttp.BuildConfig;
import com.zjb.rxokhttp.bean.HttpParams;
import com.zjb.rxokhttp.core.IParametersGenerator;

import java.lang.reflect.Field;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * time: 2015/10/16
 * description:添加默认参数和进行签名
 *
 * @author fandong
 */
public class BaseParameterGenerator implements IParametersGenerator {
    private static final Object sLock = new Object();

    @Override
    public void addBaseParameters(HttpParams parameter) {
        try {
            SdkEntity entity = AppInitManager.getInstance().getSdkEntity();
            Field[] fields = entity.getClass().getDeclaredFields();
            StringBuilder builder = new StringBuilder();
            for (Field field : fields) {
                field.setAccessible(true);
                String fieldName = field.getName();
                Object fieldObj = field.get(entity);
                if (fieldObj != null && fieldObj.getClass() == String.class) {
                    String fieldValue = (String) fieldObj;
                    if (BuildConfig.DEBUG) {
                        builder.append(fieldName)
                                .append(":")
                                .append(fieldValue)
                                .append(",");
                    }
                    parameter.put(fieldName, fieldValue);
                }
            }
            Logger.e("Request", "默认参数:" + builder.toString());
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }


    public List<String> getListParameters(boolean needEncode, HttpParams parameter) {
        List<String> list = new ArrayList<String>();
        try {
            Map<String, String> params = parameter.getTextParams();
            if (ValidateUtil.isValidate(params)) {
                for (Map.Entry<String, String> entry : params.entrySet()) {
                    String key = entry.getKey();
                    String value = entry.getValue();
                    if (!TextUtils.isEmpty(key)) {
                        if (needEncode) {
                            value = URLEncoder.encode(value, "UTF-8");
                            //这里对*做特殊处理，是因为在URLEncode转码的过程当中，*是不会进行转码的，这一点与服务器
                            //的转码方式不一样，所以需要替换一下
                            if (!TextUtils.isEmpty(value)) {
                                value = value.replaceAll("\\*", "%2A");
                            }
                        }
                        String join = key + "=" + value;
                        if (!list.contains(join)) {
                            list.add(join);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }


    @Override
    public boolean isNeedURLEncode(String url) {
        return true;
    }

    @Override
    public HttpParams generateParameter(String url, HttpParams parameters) {
        try {
            synchronized (sLock) {
                //2.对所有的非文件参数进行排序、加密，添加sig字段
                List<String> list = getListParameters(isNeedURLEncode(url), parameters);
                if (ValidateUtil.isValidate(list)) {
                    Collections.sort(list);
                    String params = CollectionUtil.join(list);
                    if (!TextUtils.isEmpty(params)) {
                        parameters.put("sig", params);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return parameters;
    }
}
