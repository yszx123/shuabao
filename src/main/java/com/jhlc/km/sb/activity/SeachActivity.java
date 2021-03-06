package com.jhlc.km.sb.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.jhlc.km.sb.R;
import com.jhlc.km.sb.adapter.TabTresureAdapter;
import com.jhlc.km.sb.constants.Constants;
import com.jhlc.km.sb.model.AntiqueBean;
import com.jhlc.km.sb.model.CategoryBean;
import com.jhlc.km.sb.tabtresure.presenter.TresurePresenter;
import com.jhlc.km.sb.tabtresure.presenter.TresurePresenterImpl;
import com.jhlc.km.sb.tabtresure.view.TresureView;
import com.jhlc.km.sb.utils.ListUtils;
import com.jhlc.km.sb.utils.SoftInputUtils;
import com.jhlc.km.sb.utils.SqliteUtils;
import com.jhlc.km.sb.utils.StringUtils;
import com.jhlc.km.sb.utils.ToastUtils;
import com.jhlc.km.sb.view.SearchPopupWindow;
import com.umeng.analytics.MobclickAgent;

import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by licheng on 22/3/16.
 */
public class SeachActivity extends BaseActivity implements SearchPopupWindow.onClickListener, TresureView, SwipeRefreshLayout.OnRefreshListener {

    @Bind(R.id.btnBack)
    ImageView btnBack;
    @Bind(R.id.textBack)
    TextView textBack;
    @Bind(R.id.llBack)
    LinearLayout llBack;
    @Bind(R.id.editSearch)
    EditText editSearch;
    @Bind(R.id.btnSearch)
    Button btnSearch;
    @Bind(R.id.llTresureType)
    LinearLayout llTresureType;
    @Bind(R.id.llTresurePrice)
    LinearLayout llTresurePrice;
    @Bind(R.id.llTresureHot)
    LinearLayout llTresureHot;
    @Bind(R.id.llSearchCondition)
    LinearLayout llSearchCondition;
    @Bind(R.id.textType)
    TextView textType;
    @Bind(R.id.imgPriceUp)
    ImageView imgPriceUp;
    @Bind(R.id.imgPriceDown)
    ImageView imgPriceDown;
    @Bind(R.id.tabRecyler)
    RecyclerView tabRecyler;
    @Bind(R.id.tabSwipeRefresh)
    SwipeRefreshLayout tabSwipeRefresh;
    @Bind(R.id.textNotice)
    TextView textNotice;
    @Bind(R.id.llNotice)
    LinearLayout llNotice;

    private String conditionType;
    private SearchPopupWindow popupWindow;
    private List<CategoryBean> menuList;

    private SqliteUtils sqliteUtils;


    private GridLayoutManager gridLayoutManager;
    private int lastVisibleItem = 0;
    private int pageIndex = 1;
    private final String TAG = "TabPageFragment";
    private List<AntiqueBean> antiqueBeanList;
    private TresurePresenter presenter;
    private TabTresureAdapter adapter;
    private int pageSize = 8;

    private boolean isLastPage = false;

    private int pricesort = 1; //该值为1,价格由低到高,为2,价格由高到低

    private int popularsort = 1; //该值为1,人气由低到高,为2,人气由高到低

    private String INTENT_TYPE_SEARCH;

    private int REFRESH_TYPE = 1; //下拉刷新类型:1 宝贝名称搜索 2 条件搜索


    @Override
    public void initView() {
        super.initView();
        INTENT_TYPE_SEARCH = getIntent().getStringExtra(Constants.INTENT_TYPE_SEARCH);
        if (Constants.INTENT_TYPE_MENU_CONDITION.equals(INTENT_TYPE_SEARCH)) {
            llSearchCondition.setVisibility(View.VISIBLE);
            conditionType = getIntent().getStringExtra(Constants.INTENT_SEARCH_CONDITION_TYPE);
            textType.setText(conditionType);
            REFRESH_TYPE = 2;
            initData();
        } else if (Constants.INTENT_TYPE_SEARCH_BTN.equals(INTENT_TYPE_SEARCH)) {
            llSearchCondition.setVisibility(View.GONE);
            initRecyclerView();
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_layout);
        ButterKnife.bind(this);
        initView();
    }

    private void initData() {
        sqliteUtils = new SqliteUtils(SeachActivity.this);
        menuList = sqliteUtils.getCategoryList();
        popupWindow = new SearchPopupWindow(SeachActivity.this, menuList);
        popupWindow.setListener(this);
        initRecyclerView();
    }

    private void initRecyclerView() {

        antiqueBeanList = new ArrayList<>();
        presenter = new TresurePresenterImpl(this, SeachActivity.this);

        gridLayoutManager = new GridLayoutManager(SeachActivity.this, 2);

        //设置下拉刷新
        tabSwipeRefresh.setColorSchemeColors(R.color.refresh_blue,
                R.color.refresh_green, R.color.refresh_green_blue, R.color.refresh_white);
        tabSwipeRefresh.setOnRefreshListener(this);
        //第一次进入界面时候加载进度条
        tabSwipeRefresh.setProgressViewOffset(false, 0, (int) TypedValue
                .applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24, getResources()
                        .getDisplayMetrics()));

        tabRecyler.setLayoutManager(gridLayoutManager);

        adapter = new TabTresureAdapter(antiqueBeanList, SeachActivity.this);


        adapter.setListener(new TabTresureAdapter.onItenClickListener() {
            @Override
            public void onItemClick(View view, int position, String id, String name) {
                Intent intent = new Intent(SeachActivity.this, TresureDetailActivity.class);
                intent.putExtra(Constants.INTENT_ANTIQUE_ID, id);
                intent.putExtra(Constants.INTENT_ANTIQUE_NAME, name);
                startActivity(intent);
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
                    Log.i("pageIndex", pageIndex + "");
                    //根据类目网络请求数据
                    if (!isLastPage) {
                        switch (INTENT_TYPE_SEARCH) {
                            case Constants.INTENT_TYPE_SEARCH_BTN:
                                String tresureName = editSearch.getText().toString();
                                if (!StringUtils.isBlank(tresureName)) {
                                    presenter.loadTresure(pageIndex, pageSize, "", tresureName, "", "", "");
                                } else {
                                    ToastUtils.show(SeachActivity.this, Constants.TAG_NO_SEARCH_KEYWORD);
                                }
                                break;
                            case Constants.INTENT_TYPE_MENU_CONDITION:
                                presenter.loadTresure(pageIndex, pageSize, sqliteUtils.getCategoryId(conditionType), "", "", "", "");
                                break;
                            default:
                                break;
                        }
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

        switch (INTENT_TYPE_SEARCH) {
            case Constants.INTENT_TYPE_SEARCH_BTN:
                break;
            case Constants.INTENT_TYPE_MENU_CONDITION:
                onRefresh();
                break;
            default:
                break;
        }
    }

    @OnClick({R.id.llBack, R.id.btnSearch, R.id.llTresureType, R.id.llTresurePrice, R.id.llTresureHot})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.llBack:
                finish();
                break;
            case R.id.btnSearch:
                REFRESH_TYPE = 1;
                String tresureName = editSearch.getText().toString();
                pageIndex = 1;
                if (!ListUtils.isEmpty(antiqueBeanList)) {
                    antiqueBeanList.clear();
                }
                //根据类目网络请求数据
                if (!StringUtils.isBlank(tresureName)) {
                    presenter.loadTresure(pageIndex, pageSize, "", tresureName, "", "", "");
                    adapter.notifyDataSetChanged();
                } else {
                    ToastUtils.show(SeachActivity.this, Constants.TAG_NO_SEARCH_KEYWORD);
                    tabSwipeRefresh.setRefreshing(false);
                }
                SoftInputUtils.hideSoftInput(SeachActivity.this,editSearch);
                break;
            case R.id.llTresureType:
                if (popupWindow.isShowing()) {
                    popupWindow.dismiss();
                } else {
                    popupWindow.showAsDropDown(view);
                }
                break;
            case R.id.llTresurePrice:
                pricesort = (pricesort == 1 ? 2 : 1);
                pageIndex = 1;
                if (!ListUtils.isEmpty(antiqueBeanList)) {
                    antiqueBeanList.clear();
                }
                //根据类目网络请求数据
                if (REFRESH_TYPE == 1) {
                    String name = editSearch.getText().toString();
                    if (!StringUtils.isBlank(name) && !textNotice.getText().toString().equals(Constants.TAG_NO_SEARCH_RESUTL)) {
                        presenter.loadTresure(pageIndex, pageSize, "", name, "", String.valueOf(pricesort), "");
                    } else {
                        ToastUtils.show(SeachActivity.this, Constants.TAG_NO_SEARCH_KEYWORD);
                        tabSwipeRefresh.setRefreshing(false);
                    }
                } else if (REFRESH_TYPE == 2) {
                    if(!textNotice.getText().toString().equals(Constants.TAG_NO_SEARCH_RESUTL)){
                        presenter.loadTresure(pageIndex, pageSize, sqliteUtils.getCategoryId(conditionType), "", "", String.valueOf(pricesort), "");
                    }
                }
                adapter.notifyDataSetChanged();
                break;
            case R.id.llTresureHot:
                popularsort = (popularsort == 1 ? 2 : 1);
                pageIndex = 1;
                if (!ListUtils.isEmpty(antiqueBeanList)) {
                    antiqueBeanList.clear();
                }
                //根据类目网络请求数据
                if (REFRESH_TYPE == 1) {
                    String name = editSearch.getText().toString();
                    if (!StringUtils.isBlank(name) && !textNotice.getText().toString().equals(Constants.TAG_NO_SEARCH_RESUTL)) {
                        presenter.loadTresure(pageIndex, pageSize, "", name, "", "", String.valueOf(popularsort));
                    } else {
                        ToastUtils.show(SeachActivity.this, Constants.TAG_NO_SEARCH_KEYWORD);
                        tabSwipeRefresh.setRefreshing(false);
                    }
                } else if (REFRESH_TYPE == 2) {
                    if(!textNotice.getText().toString().equals(Constants.TAG_NO_SEARCH_RESUTL)){
                        presenter.loadTresure(pageIndex, pageSize, sqliteUtils.getCategoryId(conditionType), "", "", "", String.valueOf(popularsort));
                    }
                }
                adapter.notifyDataSetChanged();
                break;
            default:
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        MobclickAgent.onResume(this);
    }


    @Override
    protected void onPause() {
        super.onPause();
        MobclickAgent.onPause(this);
    }

    @Override
    public void onClick(int position, String menuTitle, String menuId) {
        textType.setText(menuTitle);
        conditionType = menuTitle;
        REFRESH_TYPE = 2;
        popupWindow.dismiss();
        pageIndex = 1;
        if (!ListUtils.isEmpty(antiqueBeanList)) {
            antiqueBeanList.clear();
        }
        presenter.loadTresure(pageIndex, pageSize, menuId, "", "", "", "");
    }

    @Override
    public void onRefresh() {
        pageIndex = 1;
        if (!ListUtils.isEmpty(antiqueBeanList)) {
            antiqueBeanList.clear();
        }
        //根据类目网络请求数据
        switch (INTENT_TYPE_SEARCH) {
            case Constants.INTENT_TYPE_SEARCH_BTN:
                String tresureName = editSearch.getText().toString();
                if (!StringUtils.isBlank(tresureName)) {
                    presenter.loadTresure(pageIndex, pageSize, "", tresureName, "", "", "");
                } else {
                    ToastUtils.show(SeachActivity.this, Constants.TAG_NO_SEARCH_KEYWORD);
                    tabSwipeRefresh.setRefreshing(false);
                }
                break;
            case Constants.INTENT_TYPE_MENU_CONDITION:
                if (REFRESH_TYPE == 1) {
                    String name = editSearch.getText().toString();
                    if (!StringUtils.isBlank(name)) {
                        presenter.loadTresure(pageIndex, pageSize, "", name, "", "", "");
                    } else {
                        ToastUtils.show(SeachActivity.this, Constants.TAG_NO_SEARCH_KEYWORD);
                        tabSwipeRefresh.setRefreshing(false);
                    }
                } else if (REFRESH_TYPE == 2) {
                    presenter.loadTresure(pageIndex, pageSize, sqliteUtils.getCategoryId(conditionType), "", "", "", "");
                }
                break;
            default:
                break;
        }
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
            textNotice.setText(Constants.TAG_NO_SEARCH_RESUTL);
        }else if(!ListUtils.isEmpty(list)){

            tabRecyler.setVisibility(View.VISIBLE);
            llNotice.setVisibility(View.GONE);
            textNotice.setText("数据");//必须设定值

            if (antiqueBeanList != null) {
//            adapter.setShowFooter(true);

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
}
