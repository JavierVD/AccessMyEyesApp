package my.project.accessmyeyesapp.ui.server_fragment_list;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class ServerFragmentViewModel extends ViewModel {

    private final MutableLiveData<String> mText;

    public ServerFragmentViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is gallery fragment");
    }

    public LiveData<String> getText() {
        return mText;
    }
}