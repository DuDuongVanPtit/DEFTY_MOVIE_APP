//package com.example.defty_movie_app.viewmodel;
//
//import androidx.lifecycle.LiveData;
//import androidx.lifecycle.MutableLiveData;
//import androidx.lifecycle.ViewModel;
//
//import com.example.defty_movie_app.data.repository.ShowonRepository;
//import retrofit2.Call;
//import retrofit2.Callback;
//import retrofit2.Response;
//
//public class ShowonViewModel extends ViewModel {
//
//    private final ShowonRepository showonRepository;
//    private final MutableLiveData<Object> showonData;
//    private final MutableLiveData<String> errorMessage;
//
//    public ShowonViewModel() {
//        showonRepository = ShowonRepository.getInstance();
//        showonData = new MutableLiveData<>();
//        errorMessage = new MutableLiveData<>();
//    }
//
//    public LiveData<Object> getShowonData() {
//        return showonData;
//    }
//
//    public LiveData<String> getErrorMessage() {
//        return errorMessage;
//    }
//
//    public void fetchShowons(int page, int size, String contentType, String contentName, Integer status) {
//        showonRepository.getApi().getAllShowons(page, size, contentType, contentName, status)
//                .enqueue(new Callback<Object>() {
//                    @Override
//                    public void onResponse(Call<Object> call, Response<Object> response) {
//                        if (response.isSuccessful()) {
//                            showonData.setValue(response.body());
//                            System.out.println(response.body());
//                        } else {
//                            errorMessage.setValue("Error: " + response.message());
//                        }
//                    }
//
//                    @Override
//                    public void onFailure(Call<Object> call, Throwable t) {
//                        errorMessage.setValue("Failed: " + t.getMessage());
//                    }
//                });
//    }
//}
