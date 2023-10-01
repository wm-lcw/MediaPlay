# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
# ���ӻ�������
-optimizationpasses 5                       # ���������ѹ��������ֵ����0-7��Ĭ��5
-verbose                                    # ����ʱ��¼��־
-dontoptimize                               # ���Ż���������ļ�
-dontshrink                                 # �ر�ѹ��
#-dontpreverify                              # �ر�ԤУ��(������Javaƽ̨��Android����Ҫ��ȥ���ɼӿ����)
-dontoptimize                               # �رմ����Ż�
-dontobfuscate                              # �رջ���
-ignorewarnings                             # ���Ծ���
-dontwarn com.squareup.okhttp.**            # ָ���಻���������Ϣ
-dontusemixedcaseclassnames                 # ���������Ͷ�ΪСд
-dontskipnonpubliclibraryclasses            # �������ǹ����Ŀ����
-printmapping mapping.txt                   # ����ԭ�����������������ӳ���ļ�mapping.txt
-useuniqueclassmembernames                  # �ѻ������еķ�����Ҳ����
-allowaccessmodification                    # �Ż�ʱ������ʲ��޸������η����༰��ĳ�Ա
-renamesourcefileattribute SourceFile       # ��Դ���������������ת����SourceFile�����ڻ��������������
-keepattributes SourceFile,LineNumberTable  # �����к�
-keepattributes *Annotation*,InnerClasses,Signature,EnclosingMethod # �������ע�⡢�ڲ��ࡢ���͡�������
-optimizations !code/simplification/cast,!field/ ,!class/merging/   # ָ������ʱ���õ��㷨