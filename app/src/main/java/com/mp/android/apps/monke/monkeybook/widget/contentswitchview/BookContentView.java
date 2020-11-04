package com.mp.android.apps.monke.monkeybook.widget.contentswitchview;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.mp.android.apps.monke.monkeybook.ReadBookControl;
import com.mp.android.apps.R;
import com.mp.android.apps.monke.monkeybook.widget.MTextView;

import java.util.List;

public class BookContentView extends FrameLayout {
    public long qTag = System.currentTimeMillis();

    public static final int DURPAGEINDEXBEGIN = -1;
    public static final int DURPAGEINDEXEND = -2;

    private View view;
    private ImageView ivBg;
    private TextView tvTitle;
    private LinearLayout llContent;
    private MTextView tvContent;
    private View vBottom;
    private TextView tvPage;

    private TextView tvLoading;
    private LinearLayout llError;
    private TextView tvErrorInfo;
    private TextView tvLoadAgain;

    private String title;
    private String content;
    private int durChapterIndex;
    private int chapterAll;
    private int durPageIndex;      //如果durPageIndex = -1 则是从头开始  -2则是从尾开始
    private int pageAll;

    private ContentSwitchView.LoadDataListener loadDataListener;

    private SetDataListener setDataListener;

    public interface SetDataListener {
        public void setDataFinish(BookContentView bookContentView, int durChapterIndex, int chapterAll, int durPageIndex, int pageAll, int fromPageIndex);
    }

    public BookContentView(Context context) {
        this(context, null);
    }

    public BookContentView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BookContentView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public BookContentView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        view = LayoutInflater.from(getContext()).inflate(R.layout.adapter_content_switch_item, this, false);
        addView(view);
        ivBg = (ImageView) view.findViewById(R.id.iv_bg);
        tvTitle = (TextView) view.findViewById(R.id.tv_title);
        llContent = (LinearLayout) view.findViewById(R.id.ll_content);
        tvContent = (MTextView) view.findViewById(R.id.tv_content);
        vBottom = view.findViewById(R.id.v_bottom);
        tvPage = (TextView) view.findViewById(R.id.tv_page);

        tvLoading = (TextView) view.findViewById(R.id.tv_loading);
        llError = (LinearLayout) view.findViewById(R.id.ll_error);
        tvErrorInfo = (TextView) view.findViewById(R.id.tv_error_info);
        tvLoadAgain = (TextView) view.findViewById(R.id.tv_load_again);

        tvLoadAgain.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (loadDataListener != null)
                    loading();
            }
        });
    }

    public void loading() {
        llError.setVisibility(GONE);
        tvLoading.setVisibility(VISIBLE);
        llContent.setVisibility(INVISIBLE);
        qTag = System.currentTimeMillis();
        //执行请求操作
        if (loadDataListener != null) {
            loadDataListener.loaddata(this, qTag, durChapterIndex, durPageIndex);
        }
    }

    /**
     * 加载完成
     * 错误界面及loading标识隐藏
     * llContent内容展示显示
     */
    public void finishLoading() {
        llError.setVisibility(GONE);
        llContent.setVisibility(VISIBLE);
        tvLoading.setVisibility(GONE);
    }

    public void setNoData(String contentLines) {
        this.content = contentLines;

        tvPage.setText((this.durPageIndex + 1) + "/" + this.pageAll);

        finishLoading();
    }

    /**
     * 更新ContentView内容
     *
     * @param tag             图书来源
     * @param title           基于章节名称获取到的文章
     * @param contentLines    当前ContentView展示的数据源
     * @param durChapterIndex 当前章节(序号 int)
     * @param chapterAll      总章节数
     * @param durPageIndex    当前章节的当前页(page数)
     * @param durPageAll      基于屏幕宽度和字体设置划分的当前章节的总页数
     */
    public void updateData(long tag, String title, List<String> contentLines, int durChapterIndex, int chapterAll, int durPageIndex, int durPageAll) {
        if (tag == qTag) {
            if (setDataListener != null) {
                setDataListener.setDataFinish(this, durChapterIndex, chapterAll, durPageIndex, durPageAll, this.durPageIndex);
            }
            if (contentLines == null) {
                this.content = "";
            } else {
                StringBuilder s = new StringBuilder();
                for (int i = 0; i < contentLines.size(); i++) {
                    s.append(contentLines.get(i));
                }
                this.content = s.toString();
            }
            this.title = title;
            this.durChapterIndex = durChapterIndex;
            this.chapterAll = chapterAll;
            this.durPageIndex = durPageIndex;
            this.pageAll = durPageAll;

            tvTitle.setText(this.title);
            tvContent.setText(this.content);
            tvPage.setText((this.durPageIndex + 1) + "/" + this.pageAll);

            finishLoading();
        }
    }

    public void loadData(String title, int durChapterIndex, int chapterAll, int durPageIndex) {
        this.title = title;
        this.durChapterIndex = durChapterIndex;
        this.chapterAll = chapterAll;
        this.durPageIndex = durPageIndex;
        tvTitle.setText(title);
        tvPage.setText("");

        loading();
    }

    public ContentSwitchView.LoadDataListener getLoadDataListener() {
        return loadDataListener;
    }

    public void setLoadDataListener(ContentSwitchView.LoadDataListener loadDataListener, SetDataListener setDataListener) {
        this.loadDataListener = loadDataListener;
        this.setDataListener = setDataListener;
    }

    public void setLoadDataListener(ContentSwitchView.LoadDataListener loadDataListener) {
        this.loadDataListener = loadDataListener;
    }

    public void loadError() {
        llError.setVisibility(VISIBLE);
        tvLoading.setVisibility(GONE);
        llContent.setVisibility(INVISIBLE);
    }

    public int getPageAll() {
        return pageAll;
    }

    public void setPageAll(int pageAll) {
        this.pageAll = pageAll;
    }

    public int getDurPageIndex() {
        return durPageIndex;
    }

    public void setDurPageIndex(int durPageIndex) {
        this.durPageIndex = durPageIndex;
    }

    public int getDurChapterIndex() {
        return durChapterIndex;
    }

    public void setDurChapterIndex(int durChapterIndex) {
        this.durChapterIndex = durChapterIndex;
    }

    public int getChapterAll() {
        return chapterAll;
    }

    public void setChapterAll(int chapterAll) {
        this.chapterAll = chapterAll;
    }

    public SetDataListener getSetDataListener() {
        return setDataListener;
    }

    public void setSetDataListener(SetDataListener setDataListener) {
        this.setDataListener = setDataListener;
    }

    public long getqTag() {
        return qTag;
    }

    public void setqTag(long qTag) {
        this.qTag = qTag;
    }

    public TextView getTvContent() {
        return tvContent;
    }

    /**
     * 基于TextView界面计算 计算每页能够展示多少行内容
     *
     * @param height
     * @return
     */
    public int getLineCount(int height) {
        float ascent = tvContent.getPaint().ascent();
        float descent = tvContent.getPaint().descent();
        float textHeight = descent - ascent;
        return (int) ((height * 1.0f - tvContent.getLineSpacingExtra()) / (textHeight + tvContent.getLineSpacingExtra()));
    }

    /**
     * 设置阅读界面字体及背景等相关功能
     *
     * @param readBookControl
     */
    public void setReadBookControl(ReadBookControl readBookControl) {
        setTextKind(readBookControl);
        setBg(readBookControl);
    }

    /**
     * 设置阅读界面背景色
     *
     * @param readBookControl
     */
    public void setBg(ReadBookControl readBookControl) {
        ivBg.setImageResource(readBookControl.getTextBackground());
        tvTitle.setTextColor(readBookControl.getTextColor());
        tvContent.setTextColor(readBookControl.getTextColor());
        tvPage.setTextColor(readBookControl.getTextColor());
        vBottom.setBackgroundColor(readBookControl.getTextColor());
        tvLoading.setTextColor(readBookControl.getTextColor());
        tvErrorInfo.setTextColor(readBookControl.getTextColor());
    }

    /**
     * 设置文字属性
     *
     * @param readBookControl
     */
    public void setTextKind(ReadBookControl readBookControl) {
        tvContent.setTextSize(readBookControl.getTextSize());
        tvContent.setLineSpacing(readBookControl.getTextExtra(), 1);
    }
}
