License
=======

    Copyright 2017 Quintype, Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.


Installation
============

Using Gradle
Just add this line to list of gradle dependencies

`compile 'com.quintype:quintype-player:1.0.3'`

like this

```
dependencies {
  ...
  compile 'com.quintype:quintype-player:1.0.3'
  ...
  }
```

Usage
=====

After the integration is complete and you have successfully added the Media player module to your project; you need to use the Api's provided by the media player via the `MainPresenter`.
You can get access to `MainPresenter` by initializing it using a `.init()` mehod

```
public class ExampleApplication extends Application {
    MainPresenter presenter;

    @Override
    public void onCreate() {
        super.onCreate();
        OneSignal.startInit(this).init();
        presenter = MainPresenter.init(this, MainActivity.class);
    }

    public MainPresenter getPresenter() {
        return presenter;
    }
}
```
The `MainPresenter` should ideally be initialised in the `onCreate()` of your  `Application` class. This is useful because this will create a `MediaPlayer` which is alive as long as your application is alive. and there is a common access point to the `MediaPlayer`.

`MainPresenter` will be your single point of contact with the Media player, you can gt more information on the methods available in `MainPresenter` here.
All of the events related to the media player can be handled by implementing `UIinteractor`.

The easiest way to integrate the media player is to extend your `Activity` by `PlayerActivity`.

```
public abstract class PlayerActivity extends AppCompatActivity implements UIinteractor {

    MainPresenter presenter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        presenter = ((TemplateApplication) getApplication()).getPresenter();
        presenter.addInteractor(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        presenter.startService();
    }

    @Override
    protected void onStop() {
        super.onStop();
        presenter.unBindService();
    }

    @Override
    protected void onDestroy() {
        presenter.removeInteractor(this);
        super.onDestroy();
    }
}
```

extending this class will handle staring the `Service` then binding the service to your application and Also will provide all the callbacks necessary to handle UI changes from the media player.

