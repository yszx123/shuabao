package com.jhlc.km.sb.fragment;


import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;
import com.jhlc.km.sb.R;
import com.jhlc.km.sb.SbApplication;
import com.jhlc.km.sb.activity.CommentThumbActivity;
import com.jhlc.km.sb.activity.LoginActivity;
import com.jhlc.km.sb.activity.OpinionFeedBackActivity;
import com.jhlc.km.sb.activity.PersonnalProfileActivity;
import com.jhlc.km.sb.activity.PublishTresureActivity;
import com.jhlc.km.sb.activity.TresureDetailActivity;
import com.jhlc.km.sb.adapter.TabTresureMeFragmentAdapter;
import com.jhlc.km.sb.common.ServerInterfaceHelper;
import com.jhlc.km.sb.constants.Constants;
import com.jhlc.km.sb.model.AntiqueBean;
import com.jhlc.km.sb.model.CategoryBean;
import com.jhlc.km.sb.model.LikeAntiqueCountModel;
import com.jhlc.km.sb.tabtresure.presenter.TresurePresenter;
import com.jhlc.km.sb.tabtresure.presenter.TresurePresenterImpl;
import com.jhlc.km.sb.tabtresure.view.TresureView;
import com.jhlc.km.sb.utils.CommomUtils;
import com.jhlc.km.sb.utils.ListUtils;
import com.jhlc.km.sb.utils.PreferencesUtils;
import com.jhlc.km.sb.utils.SoftInputUtils;
import com.jhlc.km.sb.utils.SqliteUtils;
import com.jhlc.km.sb.utils.StringUtils;
import com.jhlc.km.sb.utils.ToastUtils;
import com.jhlc.km.sb.view.SearchPopupWindow;
import com.tencent.mm.sdk.modelmsg.SendMessageToWX;
import com.tencent.mm.sdk.modelmsg.WXMediaMessage;
import com.tencent.mm.sdk.modelmsg.WXWebpageObject;
import com.tencent.mm.sdk.openapi.IWXAPI;
import com.tencent.open.utils.Util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by licheng on 21/3/16.
 */
public class MeFragment extends BaseFragment implements SearchPopupWindow.onClickListener, TresureView, SwipeRefreshLayout.OnRefreshListener, ServerInterfaceHelper.Listenter {

    @Bind(R.id.sdUserHead)
    SimpleDraweeView sdUserHead;
    @Bind(R.id.btnOpinionBack)
    TextView btnOpinionBack;
    @Bind(R.id.textUserName)
    TextView textUserName;
    @Bind(R.id.textAddress)
    TextView textAddress;
    @Bind(R.id.llUserName)
    LinearLayout llUserName;
    @Bind(R.id.textOpinion)
    TextView textOpinion;
    @Bind(R.id.rlMeUserHead)
    RelativeLayout rlMeUserHead;
    @Bind(R.id.llSearchCondition)
    LinearLayout llSearchCondition;
    @Bind(R.id.editSearch)
    EditText editSearch;
    @Bind(R.id.rlSearch)
    RelativeLayout rlSearch;
    @Bind(R.id.ibtnGo)
    ImageButton ibtnGo;
    @Bind(R.id.ibtnSearch)
    ImageButton ibtnSearch;
    @Bind(R.id.imgLocation)
    ImageView imgLocation;
    @Bind(R.id.tabRecyler)
    RecyclerView tabRecyler;
    @Bind(R.id.tabSwipeRefresh)
    SwipeRefreshLayout tabSwipeRefresh;
    @Bind(R.id.textSearch)
    TextView textSearch;
    @Bind(R.id.textNotice)
    TextView textNotice;
    @Bind(R.id.llNotice)
    LinearLayout llNotice;
    @Bind(R.id.llRecycler)
    LinearLayout llRecycler;

    private List<CategoryBean> menuList;
    private SearchPopupWindow popupWindow;

    private GridLayoutManager gridLayoutManager;
    private int lastVisibleItem = 0;
    private int pageIndex = 1;
    private final String TAG = "MeFragment";
    private List<AntiqueBean> antiqueBeanList;
    private TresurePresenter presenter;
    private TabTresureMeFragmentAdapter adapter;
    private int pageSize = 8;

    private boolean isLastPage = false;

    private ServerInterfaceHelper helper;

    private SqliteUtils sqliteUtils;

    //变化的item位置
    private int positionChanged = 0;

    private boolean isSearch = false;//判断是否搜索

    private IWXAPI api;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sqliteUtils = new SqliteUtils(getActivity());
        menuList = sqliteUtils.getCategoryList();
        popupWindow = new SearchPopupWindow(getActivity(), menuList);
        popupWindow.setListener(this);
        presenter = new TresurePresenterImpl(this, getActivity());
        antiqueBeanList = new ArrayList<>();
        helper = new ServerInterfaceHelper(this, getActivity());
        api = ((SbApplication)getActivity().getApplication()).api;
    }


    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_layout_me_new, null);
        ButterKnife.bind(this, view);
        initView();

        gridLayoutManager = new GridLayoutManager(getActivity(), 2);

        //设置下拉刷新
        tabSwipeRefresh.setColorSchemeColors(R.color.refresh_blue,
                R.color.refresh_green, R.color.refresh_green_blue, R.color.refresh_white);
        tabSwipeRefresh.setOnRefreshListener(this);
        //第一次进入界面时候加载进度条
        tabSwipeRefresh.setProgressViewOffset(false, 0, (int) TypedValue
                .applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24, getResources()
                        .getDisplayMetrics()));

        tabRecyler.setLayoutManager(gridLayoutManager);

        adapter = new TabTresureMeFragmentAdapter(antiqueBeanList, getActivity());


        adapter.setListener(new TabTresureMeFragmentAdapter.onItenClickListener() {
            @Override
            public void onItemClick(View view, int position, String id, String name) {
                Intent intent = new Intent(getActivity(), TresureDetailActivity.class);
                intent.putExtra(Constants.INTENT_ANTIQUE_ID, id);
                intent.putExtra(Constants.INTENT_ANTIQUE_NAME, name);
                startActivity(intent);
            }

            @Override
            public void onDeleteClick(int position, final String id) { //删除
                positionChanged = position;
                Log.i("id",id+"");
                if(!StringUtils.isBlank(id)){
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setMessage("确定删除宝贝?");
                    builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            helper.deleteAntique(TAG, id);
                        }
                    });
                    builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
                    builder.create().show();
                }
            }

            @Override
            public void onEditClick(int position, String id, String content, String price, String picUrl) { //编辑
                Intent intent = new Intent(getActivity(), PublishTresureActivity.class);
                intent.putExtra(Constants.INTENT_TYPE_PUBLISH_TRESURE, Constants.INTENT_TYPE_PUBLISH_TRESURE_EDIT);
                intent.putExtra(Constants.INTENT_MEFRAGMENT_PUBLISH_TRESURE_ID, id);
                intent.putExtra(Constants.INTENT_MEFRAGMENT_PUBLISH_TRESURE_CONTENT, content);
                Log.i("flag",picUrl);
                intent.putExtra(Constants.INTENT_MEFRAGMENT_PUBLISH_TRESURE_IMGURL, picUrl);
                intent.putExtra(Constants.INTENT_MEFRAGMENT_PUBLISH_TRESURE_PRICE, price);
                startActivity(intent);
            }

            @Override
            public void onShareClick(int position, final String id, final String content, final String tresureIndexImage) { //分享
                if(!(StringUtils.isBlank(id) || StringUtils.isBlank(content))){
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            WXWebpageObject webpage = new WXWebpageObject();
                            webpage.webpageUrl = Constants.WEB_WECHAT_URL+id;

                            WXMediaMessage msgWx = new WXMediaMessage(webpage);
                            msgWx.title = content.substring(0,6);
                            msgWx.description = content;

                            Bitmap thumb = CommomUtils.getBitmap(tresureIndexImage); //请求网络图片地址转换为bitmap
                            msgWx.thumbData = CommomUtils.bmpToByteArray(thumb,true);

                            SendMessageToWX.Req req = new SendMessageToWX.Req();
                            req.transaction = buildTransaction("webpage");
                            req.message = msgWx;
                            req.scene = SendMessageToWX.Req.WXSceneTimeline; //分享到朋友圈

                            api.sendReq(req);
                        }
                    }).start();
                }
            }

            @Override
            public void onItemLongClick(View view, int position) {

            }
        });

        tabRecyler.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == RecyclerView.SCROLL_STATE_IDLE
                        && lastVisibleItem + 1 == adapter.getItemCount()) {
//                    swipeRefreshLayout.setRefreshing(true);
                    // 此处在现实项目中，请换成网络请求数据代码，sendRequest .....
                    Log.i("MeFragment", "pageIndex:" + pageIndex);
                    //根据类目网络请求数据
                    if (!isLastPage) {
                        presenter.loadTresure(pageIndex, pageSize, "", "", PreferencesUtils.getString(getActivity(), Constants.PREFERENCES_USER_LOGINID), "", "");
                    }
                }
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                lastVisibleItem = gridLayoutManager.findLastVisibleItemPosition();
            }
        });

        tabRecyler.setAdapter(adapter);

        return view;
    }

    Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case 1:
                    AntiqueBean antiqueBean = (AntiqueBean) msg.obj;
                    WXWebpageObject webpage = new WXWebpageObject();
                    webpage.webpageUrl = Constants.WEB_WECHAT_URL+antiqueBean.getId();

                    WXMediaMessage msgWx = new WXMediaMessage(webpage);
                    msgWx.title = antiqueBean.getDescribe().substring(0,6);
                    msgWx.description = antiqueBean.getDescribe();

                    Bitmap thumb = CommomUtils.getBitmap(antiqueBean.getIndeximage());
                    msgWx.thumbData = CommomUtils.bmpToByteArray(thumb,true);

                    SendMessageToWX.Req req = new SendMessageToWX.Req();
                    req.transaction = buildTransaction("webpage");
                    req.message = msgWx;
                    req.scene = SendMessageToWX.Req.WXSceneTimeline; //分享到朋友圈

                    api.sendReq(req);
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    public void initView() {
        super.initView();
//        initUserView();
    }

    @Override
    public void onResume() {
        super.onResume();
        initUserView();
        //如果编辑宝贝后返回不需要初始化recyclerview 待实现
        initRecylerView();
    }

    private String buildTransaction(final String type) {
        return (type == null) ? String.valueOf(System.currentTimeMillis()) : type + System.currentTimeMillis();
    }

    private void initRecylerView() {
        if (PreferencesUtils.getBoolean(getActivity(), Constants.PREFERENCES_IS_LOGIN)) {
            llRecycler.setVisibility(View.VISIBLE);
            llNotice.setVisibility(View.GONE);
            onRefresh();
        } else {
            llRecycler.setVisibility(View.GONE);
            llNotice.setVisibility(View.VISIBLE);
            textNotice.setText(Constants.TAG_FRAGMENT_LOGIN_NOTICE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ButterKnife.unbind(this);
    }

    @OnClick({R.id.btnOpinionBack, R.id.llSearchCondition, R.id.ibtnGo, R.id.ibtnSearch, R.id.textOpinion, R.id.sdUserHead})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btnOpinionBack:
                Intent opinion = new Intent(getActivity(), OpinionFeedBackActivity.class);
                startActivity(opinion);
                break;
            case R.id.llSearchCondition:
                if (popupWindow.isShowing()) {
                    popupWindow.dismiss();
                } else {
                    popupWindow.showAsDropDown(view);
                }
                break;
            case R.id.ibtnGo:
            case R.id.sdUserHead:
                if (PreferencesUtils.getBoolean(getActivity(), Constants.PREFERENCES_IS_LOGIN)) {
                    Intent personal = new Intent(getActivity(), PersonnalProfileActivity.class);
                    startActivity(personal);
                } else {
                    Intent login = new Intent(getActivity(), LoginActivity.class);
                    startActivity(login);
                }

                break;
            case R.id.ibtnSearch:
                isSearch = true;
                pageIndex = 1;
                if(!ListUtils.isEmpty(antiqueBeanList)){
                    antiqueBeanList.clear();
                }
                String name = editSearch.getText().toString();
                if (!StringUtils.isBlank(name)) {
                    presenter.loadTresure(pageIndex, pageSize, "", name, PreferencesUtils.getString(getActivity(), Constants.PREFERENCES_USERID), "", "");
                }else {
                    ToastUtils.show(getActivity(),Constants.TAG_NO_SEARCH_KEYWORD);
                }
                SoftInputUtils.hideSoftInput(getActivity(), editSearch);
                break;
            case R.id.textOpinion:
                Intent commentthumb = new Intent(getActivity(), CommentThumbActivity.class);
                startActivity(commentthumb);
                break;
        }
    }

    private void initUserView() {
        if (PreferencesUtils.getBoolean(getActivity(), Constants.PREFERENCES_IS_LOGIN)) {
            sdUserHead.setImageURI(Uri.parse(PreferencesUtils.getString(getActivity(), Constants.PREFERENCES_USER_HEADIMG) + Constants.OSS_IMAGE_SIZE200));
            imgLocation.setVisibility(View.VISIBLE);
            textAddress.setVisibility(View.VISIBLE);
            textUserName.setText(PreferencesUtils.getString(getActivity(), Constants.PREFERENCES_USERNAME));
            textAddress.setText(PreferencesUtils.getString(getActivity(), Constants.PREFERENCES_USER_ADDRESS));
            //获取点赞宝贝数量
            helper.computeByUserID(TAG);
        } else {
            imgLocation.setVisibility(View.GONE);
            textAddress.setVisibility(View.GONE);
            sdUserHead.setImageURI(Uri.parse(Constants.OSS_IMAGE_URL+Constants.OSS_DEFAULT_IMG_USERHEAD));
            textUserName.setText("去登陆");
            textOpinion.setText("宝贝0件,被赞0次");
        }
    }

    @Override
    public void onClick(int position, String menuTitle, String menuId) {
        textSearch.setText(menuTitle);
        isSearch = true;
        pageIndex = 1;
        if(!ListUtils.isEmpty(antiqueBeanList)){
            antiqueBeanList.clear();
        }
        presenter.loadTresure(pageIndex, pageSize, menuId, "", PreferencesUtils.getString(getActivity(), Constants.PREFERENCES_USERID), "", "");
        popupWindow.dismiss();
    }

    @Override
    public void onRefresh() {
        textSearch.setText("全部");
        isSearch = false;
        pageIndex = 1;
        if(!ListUtils.isEmpty(antiqueBeanList)){
            antiqueBeanList.clear();
        }
        //根据类目网络请求数据
        presenter.loadTresure(pageIndex, pageSize, "", "", PreferencesUtils.getString(getActivity(), Constants.PREFERENCES_USERID), "", "");
        adapter.notifyDataSetChanged();
    }

    @Override
    public void showProgress() {
        if (pageIndex == 1) {
            tabSwipeRefresh.setRefreshing(false);
        } else {
            tabSwipeRefresh.setRefreshing(true);
        }
    }

    @Override
    public void addTresure(List<AntiqueBean> list) {
        if (pageIndex == 1 && ListUtils.isEmpty(list)) {
            tabRecyler.setVisibility(View.GONE);
            llNotice.setVisibility(View.VISIBLE);
            if(isSearch){
                tabSwipeRefresh.setVisibility(View.VISIBLE);
                tabSwipeRefresh.setRefreshing(false);
                textNotice.setText(Constants.TAG_NO_SEARCH_RESUTL);
            }else {
                tabSwipeRefresh.setVisibility(View.GONE);
                textNotice.setText(Constants.TAG_FRAGMENT_REPROT);
            }
        }else if(!ListUtils.isEmpty(list)){
            if (antiqueBeanList != null) {
//            adapter.setShowFooter(true);

                tabSwipeRefresh.setVisibility(View.VISIBLE);
                tabRecyler.setVisibility(View.VISIBLE);
                llNotice.setVisibility(View.GONE);

                antiqueBeanList.addAll(list);

                adapter.notifyDataSetChanged();

                if (list.size() < pageSize) {
                    isLastPage = true;
                } else {
                    isLastPage = false;
                    pageIndex++;
                }

            }
        }else {
            isLastPage = true;
        }
    }

    @Override
    public void hideProgress() {
        if(tabSwipeRefresh != null){
            tabSwipeRefresh.setRefreshing(false);
        }
    }

    @Override
    public void showLoadFailMsg() {
        if (pageIndex == 1) {
//            adapter.setShowFooter(false);
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    public void success(Object object) {
        if (object instanceof String && object.equals(Constants.STATUS_SUCCESS)) { //删除成功
//            adapter.notifyItemRemoved(positionChanged);
            onRefresh();
            initUserView();
        } else if (object instanceof LikeAntiqueCountModel) {
            LikeAntiqueCountModel model = (LikeAntiqueCountModel) object;
            textOpinion.setText("宝贝" + model.getAntiquecount() + "件,被赞" + model.getLikecount() + "次");
        }
    }

    @Override
    public void failure(String status) {

    }
}