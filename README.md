# HookActivityTest

简单的了解一下插件话：通过占坑技术实现启动一个没有在配置清单注册的Activity.

### <font color=#990000>背景了解一下：</font>

我们都知道activity的启动是一个Ibinder的机制，Ibinder是两个进程通讯一个本地一个远程。

本地的ActivityManagerProxy--------对应-----------------》远程的  ActivityManagerService

我们调用startActivity，其实是通过本地的ActivityManagerProxy,ActivityManagerProxy交给ActivityManagerService，让ActivityManagerService来处理。

ActivityManagerServer一顿操作，需要启动Activity了
这个时候
ActivityManagerServer中的IApplicationThread是客户端-------------------对应----------》远程的ActivityThread中的ApplicationThread

activityThread中的ApplicationThread通过hander属性发Message，让activityThread处理handleMessage,hanleMessage根据message会判断是启动actiity还是destory掉activity.

### 思路

既然我们知道了activity启动的流程。我们就知道
1.需要在我们本地的时候调用startActivity的时候，把真正的intent隐藏起来，设置一个代理的intent
```
Intent newIntent = new Intent(MainActivity.this, HoldActivity.class);
newIntent.putExtra("realIntent", intent);
```
                   
这样当我们启动的一个没有注册过的activity的时候，就可以就不报错了“activity声明没有找到是否在配置文件中，配置了？"，这个时候会启动我们的占坑的activity.
那我们时候时候把真正的intnet恢复回来呢？
答案是，对在activityThread的mH继续hook,我们设置一个callback,当message是启动activity的时候，我们从intent中拿到真正的itent
```
Intent intent = (Intent) intentField.get(r);
Intent realIntent = intent.getParcelableExtra("realIntent");
```              
