
package com.mp.android.apps.book.presenter.impl;

import android.widget.Toast;

import androidx.annotation.NonNull;

import com.hwangjr.rxbus.RxBus;
import com.hwangjr.rxbus.annotation.Subscribe;
import com.hwangjr.rxbus.annotation.Tag;
import com.hwangjr.rxbus.thread.EventThread;
import com.mp.android.apps.basemvplib.IView;
import com.mp.android.apps.basemvplib.impl.BaseActivity;
import com.mp.android.apps.basemvplib.impl.BasePresenterImpl;
import com.mp.android.apps.book.base.observer.SimpleObserver;
import com.mp.android.apps.book.bean.SearchBookBean;
import com.mp.android.apps.book.bean.SearchHistoryBean;
import com.mp.android.apps.book.common.RxBusTag;
import com.mp.android.apps.book.dao.SearchHistoryBeanDao;
import com.mp.android.apps.book.model.WebBookModelControl;
import com.mp.android.apps.book.presenter.ISearchPresenter;
import com.mp.android.apps.book.view.ISearchView;
import com.mp.android.apps.readActivity.bean.CollBookBean;
import com.mp.android.apps.readActivity.local.DaoDbHelper;
import com.trello.rxlifecycle2.android.ActivityEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class SearchPresenterImpl extends BasePresenterImpl<ISearchView> implements ISearchPresenter {
    public static final String TAG_KEY = "tag";
    public static final String HASMORE_KEY = "hasMore";
    public static final String HASLOAD_KEY = "hasLoad";
    public static final String DURREQUESTTIME = "durRequestTime";    //当前搜索引擎失败次数  成功一次会重新开始计数
    public static final String MAXREQUESTTIME = "maxRequestTime";   //最大连续请求失败次数

    public static final int BOOK = 2;

    private Boolean hasSearch = false;   //判断是否搜索过

    private int page = 1;
    private List<Map> searchEngine;
    private Set faildEngine = new HashSet();//搜索失败引擎集合
    private int firstSuccessEngine = -1;


    private long startThisSearchTime;
    private String durSearchKey;

    private List<CollBookBean> collBookBeans = new ArrayList<>();   //用来比对搜索的书籍是否已经添加进书架

    private Boolean isInput = false;

    public SearchPresenterImpl() {
        List<CollBookBean> temp = DaoDbHelper.getInstance().getSession().getCollBookBeanDao().queryBuilder().list();
        if (temp != null && temp.size() > 0) {
            collBookBeans.addAll(temp);
        }
    }


    @Override
    public Boolean getHasSearch() {
        return hasSearch;
    }

    @Override
    public void setHasSearch(Boolean hasSearch) {
        this.hasSearch = hasSearch;
    }

    @Override
    public void insertSearchHistory() {
        final int type = SearchPresenterImpl.BOOK;
        final String content = mView.getEdtContent().getText().toString().trim();
        Observable.create(new ObservableOnSubscribe<SearchHistoryBean>() {
            @Override
            public void subscribe(ObservableEmitter<SearchHistoryBean> e) throws Exception {
                List<SearchHistoryBean> datas = DaoDbHelper.getInstance().getSession().getSearchHistoryBeanDao()
                        .queryBuilder()
                        .where(SearchHistoryBeanDao.Properties.Type.eq(type), SearchHistoryBeanDao.Properties.Content.eq(content))
                        .limit(1)
                        .build().list();
                SearchHistoryBean searchHistoryBean = null;
                if (null != datas && datas.size() > 0) {
                    searchHistoryBean = datas.get(0);
                    searchHistoryBean.setDate(System.currentTimeMillis());
                    DaoDbHelper.getInstance().getSession().getSearchHistoryBeanDao().update(searchHistoryBean);
                } else {
                    searchHistoryBean = new SearchHistoryBean(type, content, System.currentTimeMillis());
                    DaoDbHelper.getInstance().getSession().getSearchHistoryBeanDao().insert(searchHistoryBean);
                }
                e.onNext(searchHistoryBean);
            }
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SimpleObserver<SearchHistoryBean>() {
                    @Override
                    public void onNext(SearchHistoryBean value) {
                        mView.insertSearchHistorySuccess(value);
                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                    }
                });
    }

    @Override
    public void cleanSearchHistory() {
        final int type = SearchPresenterImpl.BOOK;
        final String content = mView.getEdtContent().getText().toString().trim();
        Observable.create(new ObservableOnSubscribe<Integer>() {
            @Override
            public void subscribe(ObservableEmitter<Integer> e) throws Exception {
                int a = DaoDbHelper.getInstance().getDatabase().delete(SearchHistoryBeanDao.TABLENAME, SearchHistoryBeanDao.Properties.Type.columnName + "=? and " + SearchHistoryBeanDao.Properties.Content.columnName + " like ?", new String[]{String.valueOf(type), "%" + content + "%"});
                e.onNext(a);
            }
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SimpleObserver<Integer>() {
                    @Override
                    public void onNext(Integer value) {
                        if (value > 0) {
                            mView.querySearchHistorySuccess(null);
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                    }
                });
    }

    @Override
    public void querySearchHistory() {
        final int type = SearchPresenterImpl.BOOK;
        final String content = mView.getEdtContent().getText().toString().trim();
        Observable.create(new ObservableOnSubscribe<List<SearchHistoryBean>>() {
            @Override
            public void subscribe(ObservableEmitter<List<SearchHistoryBean>> e) throws Exception {
                List<SearchHistoryBean> datas = DaoDbHelper.getInstance().getSession().getSearchHistoryBeanDao()
                        .queryBuilder()
                        .where(SearchHistoryBeanDao.Properties.Type.eq(type), SearchHistoryBeanDao.Properties.Content.like("%" + content + "%"))
                        .orderDesc(SearchHistoryBeanDao.Properties.Date)
                        .limit(20)
                        .build().list();
                e.onNext(datas);
            }
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SimpleObserver<List<SearchHistoryBean>>() {
                    @Override
                    public void onNext(List<SearchHistoryBean> value) {
                        if (null != value)
                            mView.querySearchHistorySuccess(value);
                    }

                    @Override
                    public void onError(Throwable e) {

                    }
                });
    }

    @Override
    public int getPage() {
        return page;
    }

    @Override
    public void initPage() {
        this.page = 1;
    }

    @Override
    public void toSearchBooks(String key, Boolean fromError) {
        if (key != null) {
            durSearchKey = key;
            faildEngine.clear();
            this.startThisSearchTime = System.currentTimeMillis();
            for (int i = 0; i < searchEngine.size(); i++) {
                searchEngine.get(i).put(HASMORE_KEY, true);
                searchEngine.get(i).put(HASLOAD_KEY, false);
                searchEngine.get(i).put(DURREQUESTTIME, 1);
                searchEngine.get(i).put(MAXREQUESTTIME, 1);
            }
        }
        searchBook(durSearchKey, startThisSearchTime, fromError);
    }

    //todo 分页加载,目前遍历搜索只会搜索第一页内容,对分页内容应该进行支持
    private void searchBook(final String content, final long searchTime, Boolean fromError) {
        if (searchTime == startThisSearchTime) {
            Boolean canLoad = false;
            for (Map temp : searchEngine) {
                if ((Boolean) temp.get(HASMORE_KEY) && (int) temp.get(DURREQUESTTIME) <= (int) temp.get(MAXREQUESTTIME)) {
                    canLoad = true;
                    break;
                }
            }
            if (canLoad) {
                int searchEngineIndex = -1;
                for (int i = 0; i < searchEngine.size(); i++) {
                    if (!(Boolean) searchEngine.get(i).get(HASLOAD_KEY) && (int) searchEngine.get(i).get(DURREQUESTTIME) <= (int) searchEngine.get(i).get(MAXREQUESTTIME)) {
                        searchEngineIndex = i;
                        break;
                    }
                }
                if (searchEngineIndex == -1) {
                    this.page++;
                    for (Map item : searchEngine) {
                        item.put(HASLOAD_KEY, false);
                    }
                    if (!fromError) {
                        if (page - 1 == 1) {
                            mView.refreshFinish(false);
                        } else {
                            mView.loadMoreFinish(false);
                        }
                    } else {
                        searchBook(content, searchTime, false);
                    }
                } else {
                    final int finalSearchEngineIndex = searchEngineIndex;
                    WebBookModelControl.getInstance().searchOtherBook(content, page, (String) searchEngine.get(searchEngineIndex).get(TAG_KEY))
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribeOn(Schedulers.io())
                            .subscribe(new SimpleObserver<List<SearchBookBean>>() {
                                @Override
                                public void onNext(List<SearchBookBean> value) {
                                    if (searchTime == startThisSearchTime) {
                                        searchEngine.get(finalSearchEngineIndex).put(HASLOAD_KEY, true);
                                        searchEngine.get(finalSearchEngineIndex).put(DURREQUESTTIME, 1);
                                        if (value.size() == 0) {
                                            searchEngine.get(finalSearchEngineIndex).put(HASMORE_KEY, false);
                                        } else {
                                            for (SearchBookBean temp : value) {
                                                for (CollBookBean CollBookBean : collBookBeans) {
                                                    if (temp.getNoteUrl().equals(CollBookBean.get_id())) {
                                                        temp.setAdd(true);
                                                        break;
                                                    }
                                                }
                                            }
                                        }
                                        //增加少个成功搜索引擎id
                                        if (firstSuccessEngine == -1) {
                                            firstSuccessEngine = finalSearchEngineIndex;
                                        }
                                        if (page == 1 && finalSearchEngineIndex == firstSuccessEngine) {
                                            mView.refreshSearchBook(value);
                                        } else {
                                            if (value != null && value.size() > 0 && !mView.checkIsExist(value.get(0)))
                                                mView.loadMoreSearchBook(value);
                                            else {
                                                searchEngine.get(finalSearchEngineIndex).put(HASMORE_KEY, false);
                                            }
                                        }
                                        searchBook(content, searchTime, false);
                                    }
                                }

                                @Override
                                public void onError(Throwable e) {
                                    e.printStackTrace();
                                    if (searchTime == startThisSearchTime) {
                                        faildEngine.add(searchEngine.get(finalSearchEngineIndex).get(TAG_KEY));
                                        searchEngine.get(finalSearchEngineIndex).put(HASLOAD_KEY, false);
                                        searchEngine.get(finalSearchEngineIndex).put(DURREQUESTTIME, ((int) searchEngine.get(finalSearchEngineIndex).get(DURREQUESTTIME)) + 1);
                                        if (faildEngine.size() == searchEngine.size()) {
                                            mView.searchBookError(page == 1 && (finalSearchEngineIndex == 0 || (finalSearchEngineIndex > 0 && mView.getSearchBookAdapter().getItemcount() == 0)));
                                        } else {
                                            searchBook(content, searchTime, false);
                                        }
                                    }
                                }
                            });
                }
            } else {
                if (page == 1) {
                    mView.refreshFinish(true);
                } else {
                    mView.loadMoreFinish(true);
                }
                this.page++;
                for (Map item : searchEngine) {
                    item.put(HASLOAD_KEY, false);
                }
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void addBookToShelf(final SearchBookBean searchBookBean) {
        CollBookBean collBookBean = new CollBookBean().getCollBookBeanFromSearch(searchBookBean);
        if (collBookBean != null) {
            if (collBookBean.getBookChapterUrl() != null) {
                BookShelUtils.getInstance().addToBookShelfUtils(collBookBean);
            } else {
                WebBookModelControl.getInstance().getBookInfo(collBookBean).subscribeOn(Schedulers.io())
                        .compose(((BaseActivity) mView.getContext()).<CollBookBean>bindUntilEvent(ActivityEvent.DESTROY))
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new SimpleObserver<CollBookBean>() {
                            @Override
                            public void onNext(CollBookBean collBookBean) {
                                BookShelUtils.getInstance().addToBookShelfUtils(collBookBean);
                            }

                            @Override
                            public void onError(Throwable e) {
                                Toast.makeText(mView.getContext(), "加入书架失败", Toast.LENGTH_LONG).show();
                            }
                        });
            }
        }


    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void attachView(@NonNull IView iView) {
        super.attachView(iView);
        //搜索引擎初始化
        searchEngine = new ArrayList<>();
        WebBookModelControl.getInstance().registerSearchEngine(searchEngine, mView.getContext());
        //注册 eventBus
        RxBus.get().register(this);
    }

    @Override
    public void detachView() {
        RxBus.get().unregister(this);
    }

    @Subscribe(
            thread = EventThread.MAIN_THREAD,
            tags = {
                    @Tag(RxBusTag.HAD_ADD_BOOK)
            }
    )
    /**
     * rxbus发送消息进行接受
     */
    public void hadAddBook(CollBookBean collBookBean) {
        collBookBeans.add(collBookBean);
        List<SearchBookBean> datas = mView.getSearchBookAdapter().getSearchBooks();
        for (int i = 0; i < datas.size(); i++) {
            if (datas.get(i).getNoteUrl().equals(collBookBean.get_id())) {
                datas.get(i).setAdd(true);
                mView.updateSearchItem(i);
                break;
            }
        }
    }

    @Subscribe(
            thread = EventThread.MAIN_THREAD,
            tags = {
                    @Tag(RxBusTag.HAD_REMOVE_BOOK)
            }
    )
    public void hadRemoveBook(CollBookBean collBookBean) {
        if (collBookBeans != null) {
            for (int i = 0; i < collBookBeans.size(); i++) {
                if (collBookBeans.get(i).get_id().equals(collBookBean.get_id())) {
                    collBookBeans.remove(i);
                    break;
                }
            }
        }
        List<SearchBookBean> datas = mView.getSearchBookAdapter().getSearchBooks();
        for (int i = 0; i < datas.size(); i++) {
            if (datas.get(i).getNoteUrl().equals(collBookBean.get_id())) {
                datas.get(i).setAdd(false);
                mView.updateSearchItem(i);
                break;
            }
        }
    }

    @Override
    public Boolean getInput() {
        return isInput;
    }

    @Override
    public void setInput(Boolean input) {
        isInput = input;
    }
}
