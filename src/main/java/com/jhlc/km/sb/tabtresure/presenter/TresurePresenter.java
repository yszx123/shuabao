package com.jhlc.km.sb.tabtresure.presenter;

/**
 * Created by licheng on 21/3/16.
 */
public interface TresurePresenter {
    void loadTresure(int pageIndex, int pageSize, String categroyid, String name, String userid, String pricesort, String popularsort);
    void loadTresureFocused(int pageIndex, int pageSize);
    void loadTresureCollect(int pageIndex, int pageSize);
}