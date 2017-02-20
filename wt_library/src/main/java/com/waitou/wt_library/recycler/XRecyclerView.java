package com.waitou.wt_library.recycler;

import android.content.Context;
import android.support.annotation.ColorRes;
import android.support.annotation.DimenRes;
import android.support.annotation.Nullable;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.util.AttributeSet;
import android.view.View;

import com.waitou.wt_library.recycler.divider.HorizontalDividerItemDecoration;
import com.waitou.wt_library.recycler.divider.VerticalDividerItemDecoration;

import java.util.Collections;
import java.util.List;


/**
 * Created by wanglei on 2016/10/30.
 */

public class XRecyclerView extends RecyclerView {

    public static final int DEFAULT_PAGE_SIZE = 20;

    private float xFactor = 1.0f;
    private float yFactor = 1.0f;

    LayoutManagerType layoutManagerType;        //LayoutManager类型
    private int[] lastScrollPositions;      //瀑布流位置存储
    private int[] firstScrollPositions;

    LoadMoreUIHandler loadMoreUIHandler;
    View              loadMoreView;      //加载更多控件

    private boolean loadMore    = false; //是否加载更多
    private int     totalPage   = 1;
    private int     currentPage = 1;

    XRecyclerAdapter             adapter;
    StateCallback                stateCallback;
    OnRefreshAndLoadMoreListener onRefreshAndLoadMoreListener;

    public static final int LOAD_MORE_ITEM_SLOP = 2;

    public XRecyclerView(Context context) {
        this(context, null);
    }

    public XRecyclerView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public XRecyclerView(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setUpView();
    }

    private void setUpView() {
        addOnScrollListener(processMoreListener);
    }

    @Override
    public void setAdapter(Adapter adapter) {
        if (adapter == null) return;

        if (!(adapter instanceof XRecyclerAdapter)) {
            adapter = new XRecyclerAdapter(adapter);
        }

        super.setAdapter(adapter);

        if (adapter.getItemCount() > 0) {
            if (getStateCallback() != null) {
                getStateCallback().notifyContent();
            }
        }

        final XRecyclerAdapter finalAdapter = (XRecyclerAdapter) adapter;
        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                update();
            }

            @Override
            public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
                super.onItemRangeMoved(fromPosition, toPosition, itemCount);
                update();
            }

            @Override
            public void onItemRangeRemoved(int positionStart, int itemCount) {
                super.onItemRangeRemoved(positionStart, itemCount);
                update();
            }

            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                update();
            }

            @Override
            public void onItemRangeChanged(int positionStart, int itemCount) {
                super.onItemRangeChanged(positionStart, itemCount);
                update();
            }

            private void update() {
                int dataCount = finalAdapter.getDataCount();
                if (dataCount > 0) {
                    if (loadMore) {
                        loadMoreCompleted();
                    }
                    if (getStateCallback() != null) {
                        getStateCallback().notifyContent();
                    }
                } else {
                    if (finalAdapter.getHeaderSize() > 0 || finalAdapter.getFooterSize() > 0) {
                        if (loadMoreView != null) {
                            loadMoreView.setVisibility(GONE);
                        }
                    }
                }

                if (getStateCallback() != null) getStateCallback().refreshState(false);
            }
        });

        this.adapter = (XRecyclerAdapter) adapter;

    }

    public XRecyclerView stateCallback(StateCallback stateCallback) {
        this.stateCallback = stateCallback;
        return this;
    }

    public StateCallback getStateCallback() {
        return stateCallback;
    }

    @Override
    public XRecyclerAdapter getAdapter() {
        return adapter;
    }

    public void onRefresh() {
        currentPage = 1;
        if (getOnRefreshAndLoadMoreListener() != null) {
            getOnRefreshAndLoadMoreListener().onRefresh();
        }
    }

    @Override
    public void setLayoutManager(LayoutManager layout) {
        if (layout == null) {
            throw new IllegalArgumentException("LayoutManager can not be null.");
        }
        super.setLayoutManager(layout);

        if (layout instanceof GridLayoutManager) {
            int spanCount = ((GridLayoutManager) layout).getSpanCount();
            setSpanLookUp(layout, spanCount);
        }

        if (layout instanceof StaggeredGridLayoutManager) {
            int spanCount = ((StaggeredGridLayoutManager) layout).getSpanCount();
            setSpanLookUp(layout, spanCount);
        }
    }

    @Override
    public boolean fling(int velocityX, int velocityY) {
        return super.fling((int) (velocityX * xFactor), (int) (velocityY * yFactor));
    }

    public XRecyclerView xFactor(float xFactor) {
        this.xFactor = xFactor;
        return this;
    }

    public XRecyclerView yFactor(float yFactor) {
        this.yFactor = yFactor;
        return this;
    }

    public int getLastVisibleItemPosition() {
        return getLastVisibleItemPosition(getLayoutManager());
    }

    private int getLastVisibleItemPosition(RecyclerView.LayoutManager layoutManager) {
        int lastVisibleItemPosition = -1;
        if (layoutManagerType == null) {
            if (layoutManager instanceof LinearLayoutManager) {
                layoutManagerType = LayoutManagerType.LINEAR;
            } else if (layoutManager instanceof GridLayoutManager) {
                layoutManagerType = LayoutManagerType.GRID;
            } else if (layoutManager instanceof StaggeredGridLayoutManager) {
                layoutManagerType = LayoutManagerType.STAGGERED_GRID;
            } else {
                throw new RuntimeException("Unsupported LayoutManager used. Valid ones are LinearLayoutManager, GridLayoutManager and StaggeredGridLayoutManager");
            }
        }

        switch (layoutManagerType) {
            case LINEAR:
                lastVisibleItemPosition = ((LinearLayoutManager) layoutManager).findLastVisibleItemPosition();
                break;
            case GRID:
                lastVisibleItemPosition = ((GridLayoutManager) layoutManager).findLastVisibleItemPosition();
                break;
            case STAGGERED_GRID:
                StaggeredGridLayoutManager staggeredGridLayoutManager = (StaggeredGridLayoutManager) layoutManager;
                if (lastScrollPositions == null)
                    lastScrollPositions = new int[staggeredGridLayoutManager.getSpanCount()];

                staggeredGridLayoutManager.findLastVisibleItemPositions(lastScrollPositions);
                lastVisibleItemPosition = findMax(lastScrollPositions);
                break;
        }
        return lastVisibleItemPosition;
    }


    private int getFirstVisibleItemPosition(RecyclerView.LayoutManager layoutManager) {
        int firstVisibleItemPosition = -1;
        if (layoutManagerType == null) {
            if (layoutManager instanceof LinearLayoutManager) {
                layoutManagerType = LayoutManagerType.LINEAR;
            } else if (layoutManager instanceof GridLayoutManager) {
                layoutManagerType = LayoutManagerType.GRID;
            } else if (layoutManager instanceof StaggeredGridLayoutManager) {
                layoutManagerType = LayoutManagerType.STAGGERED_GRID;
            } else {
                throw new RuntimeException("Unsupported LayoutManager used. Valid ones are LinearLayoutManager, GridLayoutManager and StaggeredGridLayoutManager");
            }
        }

        switch (layoutManagerType) {
            case LINEAR:
                firstVisibleItemPosition = ((LinearLayoutManager) layoutManager).findFirstVisibleItemPosition();
                break;
            case GRID:
                firstVisibleItemPosition = ((GridLayoutManager) layoutManager).findFirstVisibleItemPosition();
                break;
            case STAGGERED_GRID:
                StaggeredGridLayoutManager staggeredGridLayoutManager = (StaggeredGridLayoutManager) layoutManager;
                if (lastScrollPositions == null)
                    lastScrollPositions = new int[staggeredGridLayoutManager.getSpanCount()];

                staggeredGridLayoutManager.findLastVisibleItemPositions(firstScrollPositions);
                firstVisibleItemPosition = findMin(firstScrollPositions);
                break;
        }
        return firstVisibleItemPosition;
    }


    private int findMax(int[] lastPositions) {
        int max = Integer.MIN_VALUE;
        for (int value : lastPositions) {
            if (value > max)
                max = value;
        }
        return max;
    }

    private int findMin(int[] positions) {
        int min = Integer.MIN_VALUE;
        for (int value : positions) {
            if (value < min)
                min = value;
        }
        return min;
    }


    private void setSpanLookUp(RecyclerView.LayoutManager layoutManager, final int spanCount) {
        if (layoutManager instanceof GridLayoutManager) {
            ((GridLayoutManager) layoutManager).setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                @Override
                public int getSpanSize(int position) {
                    if (adapter != null) {
                        return adapter.isHeaderOrFooter(position) ? spanCount : 1;
                    }
                    return GridLayoutManager.DEFAULT_SPAN_COUNT;
                }
            });
        } else if (layoutManager instanceof StaggeredGridLayoutManager) {
            ((StaggeredGridLayoutManager) layoutManager).setSpanCount(spanCount);
        }

    }

    public boolean addHeaderView(int position, View view) {
        boolean result = false;
        if (view == null) {
            return result;
        }
        if (adapter != null) {
            result = adapter.addHeadView(position, view);
        }
        return result;
    }

    public boolean addHeaderView(View view) {
        boolean result = false;
        if (view == null) {
            return result;
        }
        if (adapter != null) {
            result = adapter.addHeadView(view);
        }
        return result;
    }

    public boolean removeHeaderView(View view) {
        boolean result = false;
        if (view == null) {
            return result;
        }
        if (adapter != null) {
            result = adapter.removeHeadView(view);
        }
        return result;
    }

    public boolean removeAllHeaderView() {
        boolean result = false;
        if (adapter != null) {
            result = adapter.removeAllHeadersView();
        }
        return result;
    }

    public boolean addFooterView(View view) {
        boolean result = false;
        if (view == null) {
            return false;
        }
        if (adapter != null) {
            result = adapter.addFootView(view);
        }
        return result;
    }

    public boolean addFooterView(int position, View view) {
        boolean result = false;
        if (view == null) {
            return result;
        }
        if (adapter != null) {
            result = adapter.addFootView(position, view);
        }
        return result;
    }

    public boolean removeFooterView(View view) {
        boolean result = false;
        if (view == null) {
            return result;
        }
        if (adapter != null) {
            result = adapter.removeFootView(view);
        }
        return result;
    }

    public boolean removeAllFootView() {
        boolean result = false;
        if (adapter != null) {
            result = adapter.removeAllFootView();
        }
        return result;
    }

    public int getHeaderCount() {
        int count = 0;
        if (adapter != null) {
            count = adapter.getHeaderSize();
        }
        return count;
    }

    public List<View> getHeaderViewList() {
        if (adapter != null) {
            return adapter.getHeaderViewList();
        }
        return Collections.EMPTY_LIST;
    }

    public int getFooterCount() {
        int count = 0;
        if (adapter != null) {
            return adapter.getFooterSize();
        }
        return count;
    }

    public List<View> getFooterViewList() {
        if (adapter != null) {
            return adapter.getFooterViewList();
        }
        return Collections.EMPTY_LIST;
    }

    /**
     * 使用默认的加载更多
     */
    public void useDefLoadMoreView() {
        SimpleLoadMoreFooter loadMoreFooter = new SimpleLoadMoreFooter(getContext());
        setLoadMoreView(loadMoreFooter);
        setLoadMoreUIHandler(loadMoreFooter);
    }


    public void loadMoreFooterView(View view) {
        setLoadMoreView(view);
        setLoadMoreUIHandler((LoadMoreUIHandler) view);
    }

    /**
     * 设置加载更多布局
     *
     * @param view
     */
    public void setLoadMoreView(View view) {
        if (loadMoreView != null && loadMoreView != view) {
            removeFooterView(view);
        }
        loadMoreView = view;

        addFooterView(view);
    }

    public void setLoadMoreUIHandler(LoadMoreUIHandler loadMoreUIHandler) {
        this.loadMoreUIHandler = loadMoreUIHandler;
    }

    private void loadMoreCompleted() {
        if (loadMoreUIHandler != null) {
            loadMoreUIHandler.onLoadFinish(totalPage > currentPage);
        }
        loadMore = false;
        if (getStateCallback() != null) {
            getStateCallback().refreshEnabled(true);
            getStateCallback().notifyContent();
        }
    }


    public void loadMoreError(String s) {
        if (loadMoreUIHandler != null) {
            loadMoreUIHandler.onErrorMore(s);
        }
        loadMore = false;
    }


    public void setDefaultPageSize() {
        setPage(currentPage, DEFAULT_PAGE_SIZE);
    }

    public void setPage(final int currentPage, final int totalPage) {
        this.currentPage = currentPage;
        this.totalPage = totalPage;
        if (loadMoreUIHandler != null) {
            loadMoreUIHandler.onLoadFinish(totalPage > currentPage);
        }
    }

    /**
     * 刷新数据
     */
    public void refreshData() {
        if (getStateCallback() != null) getStateCallback().refreshState(true);
        if (getOnRefreshAndLoadMoreListener() != null) {
            getOnRefreshAndLoadMoreListener().onRefresh();
        }
    }

    private RecyclerView.OnScrollListener processMoreListener = new RecyclerView.OnScrollListener() {

        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            super.onScrollStateChanged(recyclerView, newState);

            if (adapter == null || recyclerView.getLayoutManager() == null) return;

            int totalCount = adapter.getItemCount();

            if (newState == RecyclerView.SCROLL_STATE_IDLE && !loadMore && getLastVisibleItemPosition(recyclerView.getLayoutManager()) + LOAD_MORE_ITEM_SLOP > totalCount) {

                if (totalPage > currentPage) {
                    loadMore = true;

                    if (getOnRefreshAndLoadMoreListener() != null) {
                        getOnRefreshAndLoadMoreListener().onLoadMore(++currentPage);

                        if (getStateCallback() != null) getStateCallback().refreshEnabled(false);

                        if (loadMoreUIHandler != null) {
                            loadMoreUIHandler.onLoading();
                        }
                    }
                } else {
                    loadMoreCompleted();
                }

            } else {
                if (getStateCallback() != null) getStateCallback().refreshEnabled(true);
            }

        }

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);

        }
    };

    public XRecyclerView setOnRefreshAndLoadMoreListener(OnRefreshAndLoadMoreListener onRefreshAndLoadMoreListener) {
        this.onRefreshAndLoadMoreListener = onRefreshAndLoadMoreListener;
        if (getStateCallback() != null) {
            getStateCallback().refreshEnabled(true);
        }
        return this;
    }

    public XRecyclerView noDivider() {
        setItemAnimator(new DefaultItemAnimator());
        setHasFixedSize(true);
        return this;
    }

    public XRecyclerView horizontalDivider(@ColorRes int colorRes, @DimenRes int dimenRes) {
        setItemAnimator(new DefaultItemAnimator());
        setHasFixedSize(true);
        addItemDecoration(new HorizontalDividerItemDecoration.Builder(getContext())
                .colorResId(colorRes)
                .size(getContext().getResources().getDimensionPixelSize(dimenRes))
                .build()
        );
        return this;
    }

    public XRecyclerView verticalDivider(@ColorRes int colorRes, @DimenRes int dimenRes) {
        setItemAnimator(new DefaultItemAnimator());
        setHasFixedSize(true);
        addItemDecoration(new VerticalDividerItemDecoration.Builder(getContext())
                .colorResId(colorRes)
                .size(getContext().getResources().getDimensionPixelSize(dimenRes))
                .build()
        );
        return this;
    }

    public int getCurrentPage() {
        return currentPage;
    }


    public OnRefreshAndLoadMoreListener getOnRefreshAndLoadMoreListener() {
        return onRefreshAndLoadMoreListener;
    }


    enum LayoutManagerType {
        LINEAR, GRID, STAGGERED_GRID
    }

    public interface StateCallback {

        void notifyContent();

        void refreshState(boolean isRefresh);

        void refreshEnabled(boolean isEnabled);
    }

    public interface OnRefreshAndLoadMoreListener {
        void onRefresh();

        void onLoadMore(int page);
    }

    public abstract class OnRefreshListener implements OnRefreshAndLoadMoreListener {
        @Override
        public void onLoadMore(int page) {

        }
    }
}