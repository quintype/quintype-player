**Using The media player**

After the integration is complete and you have succcessfully added the Media player module to your project; you need to use the Api's provided by the media player via the `MainPresenter`.
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