

package com.rishabh.emojipicker

import android.app.KeyguardManager
import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.util.Consumer
import androidx.core.view.ViewCompat
import androidx.emoji2.text.EmojiCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
//import com.rishabh.apiandfirebasejsonfile.MyInterfaces.EmojiPickedFromSuggestion
import com.rishabh.emojipicker.EmojiPickerConstants.DEFAULT_MAX_RECENT_ITEM_ROWS
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.EmptyCoroutineContext

interface EmojiPickedFromSuggestion {
    fun pickedEmoji(emoji:String)
}
/**
 * The emoji picker view that provides up-to-date emojis in a vertical scrollable view with a
 * clickable horizontal header.
 */
class EmojiPickerView
@JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : FrameLayout(context, attrs, defStyleAttr) {

    internal companion object {
        internal var emojiCompatLoaded: Boolean = false
    }
    var emojiPickedFromSuggestion:EmojiPickedFromSuggestion?=null

    private var _emojiGridRows: Float? = null
    /**
     * The number of rows of the emoji picker.
     *
     * Optional field. If not set, the value will be calculated based on parent view height and
     * [emojiGridColumns]. Float value indicates that the picker could display the last row
     * partially, so the users get the idea that they can scroll down for more contents.
     *
     * @attr ref androidx.emoji2.emojipicker.R.styleable.EmojiPickerView_emojiGridRows
     */
    var emojiGridRows: Float
        get() = _emojiGridRows ?: -1F
        set(value) {
            _emojiGridRows = value.takeIf { it > 0 }
            // Refresh when emojiGridRows is reset
            if (isLaidOut) {
                showEmojiPickerView()
            }
        }

    private var _usedInSearchResult:Boolean = false
    /*it is used to show the all the emoji and their categories if it's false else- only show emoji that is match with the description text */
    var usedInSearchResult:Boolean
        get() = _usedInSearchResult
        set(value) {
            _usedInSearchResult = value
        }

    /**
     * The number of columns of the emoji picker.
     *
     * Default value([EmojiPickerConstants.DEFAULT_BODY_COLUMNS]: 9) will be used if
     * emojiGridColumns is set to non-positive value.
     *
     * @attr ref androidx.emoji2.emojipicker.R.styleable.EmojiPickerView_emojiGridColumns
     */
    var emojiGridColumns: Int = EmojiPickerConstants.DEFAULT_BODY_COLUMNS
        set(value) {
            field = value.takeIf { it > 0 } ?: EmojiPickerConstants.DEFAULT_BODY_COLUMNS
            // Refresh when emojiGridColumns is reset
            if (isLaidOut) {
                showEmojiPickerView()
            }
        }

    private val stickyVariantProvider = StickyVariantProvider(context)
    private val scope = CoroutineScope(EmptyCoroutineContext)

    private var recentEmojiProvider: RecentEmojiProvider? = null
    private var recentNeedsRefreshing: Boolean = true
    private val recentItems: MutableList<EmojiViewData> = mutableListOf()
    private lateinit var recentItemGroup: ItemGroup

    lateinit var emojiPickerItems: EmojiPickerItems
    lateinit var bodyAdapter: EmojiPickerBodyAdapter
    lateinit var headerAdapter: EmojiPickerHeaderAdapter


    private var onEmojiPickedListener: Consumer<EmojiViewItem>? = null

    init {
        val isLocked = isDeviceLocked(context)

        if (!isLocked) {
            try {
                recentEmojiProvider = DefaultRecentEmojiProvider(context)
            } catch (e: Exception) {
                // If SharedPreferences access fails for any reason, set to null
                recentEmojiProvider = null
            }
        }

        val typedArray: TypedArray = context.obtainStyledAttributes(attrs, R.styleable.EmojiPickerView, 0, 0)
        _emojiGridRows = with(R.styleable.EmojiPickerView_emojiGridRows) {
            if (typedArray.hasValue(this)) {
                typedArray.getFloat(this, 0F)
            } else null
        }
        emojiGridColumns = typedArray.getInt(
            R.styleable.EmojiPickerView_emojiGridColumns, EmojiPickerConstants.DEFAULT_BODY_COLUMNS
        )

        usedInSearchResult =
            typedArray.getBoolean(R.styleable.EmojiPickerView_usedInSearchResult, false)
        typedArray.recycle()




    }

    fun build(color:Int,emojiesLoaded:()->Unit){



        if (EmojiCompat.isConfigured()) {
            when (EmojiCompat.get().loadState) {
                EmojiCompat.LOAD_STATE_SUCCEEDED -> emojiCompatLoaded = true
                EmojiCompat.LOAD_STATE_LOADING,
                EmojiCompat.LOAD_STATE_DEFAULT ->
                    EmojiCompat.get().registerInitCallback(object : EmojiCompat.InitCallback() {
                        override fun onInitialized() {
                            emojiCompatLoaded = true
                            scope.launch(Dispatchers.IO) {
                                BundledEmojiListLoader.load(context)
                                withContext(Dispatchers.Main) {
                                    emojiPickerItems = buildEmojiPickerItems()
                                    bodyAdapter.dynamicTextColor = color
                                    bodyAdapter.notifyDataSetChanged()
                                    emojiesLoaded()
                                }
                            }
                        }

                        override fun onFailed(throwable: Throwable?) {}
                    }
                    )
            }
        }

        scope.launch(Dispatchers.IO) {
            val load = launch { BundledEmojiListLoader.load(context) }
            refreshRecent()
            load.join()

            withContext(Dispatchers.Main) { showEmojiPickerView()
                headerAdapter.dynamicTextColor = color
                bodyAdapter.updateTextColor(color)
                emojiesLoaded()}
        }
    }
    fun isDeviceLocked(context: Context): Boolean {
        return try {
            val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.isDeviceLocked || keyguardManager.isKeyguardLocked
        } catch (e: Exception) {
            // If we can't determine lock state, assume it's locked to be safe
            true
        }
    }
    fun createEmojiPickerBodyAdapter(): EmojiPickerBodyAdapter {
        return EmojiPickerBodyAdapter(
            context,
            emojiGridColumns,
            _emojiGridRows,
            stickyVariantProvider,
            emojiPickerItemsProvider = { emojiPickerItems },
            onEmojiPickedListener = { emojiViewItem ->
                emojiPickedFromSuggestion?.pickedEmoji(emojiViewItem.emoji)
                emojiViewItem.emoji
                onEmojiPickedListener?.accept(emojiViewItem)
                recentEmojiProvider?.recordSelection(emojiViewItem.emoji)
                recentNeedsRefreshing = true
            }
        )
    }

    /*here is emoji is get from the bundle and send to recycler view*/
     fun buildEmojiPickerItems(onlyRecentEmojies: Boolean = false, description: String?=null ) :EmojiPickerItems{

        if(!usedInSearchResult){
            return EmojiPickerItems(
                buildList {
                    add(
                        ItemGroup(
                            R.drawable.quantum_gm_ic_access_time_filled_vd_theme_24,
                            CategoryTitle(context.getString(R.string.emoji_category_recent)),
                            recentItems,
                            maxContentItemCount = DEFAULT_MAX_RECENT_ITEM_ROWS * emojiGridColumns,
                            emptyPlaceholderItem =
                            PlaceholderText(
                                context.getString(R.string.emoji_empty_recent_category)
                            )
                        )
                            .also { recentItemGroup = it }
                    )

                    for ((i, category) in
                    BundledEmojiListLoader.getCategorizedEmojiData().withIndex()) {
                        add(
                            ItemGroup(
                                category.headerIconId,
                                CategoryTitle(category.categoryName),
                                category.emojiDataList.mapIndexed { j, emojiData ->

                                    EmojiViewData(
                                        stickyVariantProvider[emojiData.emoji],
                                        dataIndex = i + j
                                    )
                                },
                            )
                        )
                    }
                }
            )
        }else{
            if(onlyRecentEmojies && description==null ){
                return EmojiPickerItems(
                    buildList {
                        add(ItemGroup(
                                R.drawable.quantum_gm_ic_access_time_filled_vd_theme_24,
                                CategoryTitle(context.getString(R.string.emoji_category_recent)),
                                recentItems,
                                maxContentItemCount = DEFAULT_MAX_RECENT_ITEM_ROWS * emojiGridColumns,
                                emptyPlaceholderItem =
                                PlaceholderText(
                                    context.getString(R.string.emoji_empty_recent_category)
                                )
                            )
//                                .also { recentItemGroup = it }
                        )

                    }
                )
            }
            else if(!onlyRecentEmojies && description!=null){
                
                return EmojiPickerItems(
                    buildList {
                        val listOfEmojiData = mutableListOf<EmojiViewData>()
                        var categoryHeaderIconId:Int = 0
                        var categoryName:String = ""

                        for ((i, category) in BundledEmojiListLoader.getCategorizedEmojiData().withIndex()) {
                            for((j,emojiData) in category.emojiDataList.withIndex()){

                                if(emojiData.description.contains(description)){
                                    if(listOfEmojiData.size<17){
                                        categoryHeaderIconId = category.headerIconId
                                        listOfEmojiData.add(EmojiViewData(stickyVariantProvider[emojiData.emoji], dataIndex = i + j))
                                    }

                                }
                            }
                        }
                        add(ItemGroup(categoryHeaderIconId,
                            CategoryTitle(categoryName),listOfEmojiData)
                        )
                    })
            }else { return EmojiPickerItems(listOf()) }

        }

    }





    /**
     * Gets the recent emoji provider used by this picker.
     */
    fun getRecentEmojiProvider(): RecentEmojiProvider? {
        return recentEmojiProvider
    }



    /**
     * Forces a refresh of recent emojis from the provider.
     */
    suspend fun forceRefreshRecent() {
        recentNeedsRefreshing = true
        refreshRecent()
    }





    private fun showEmojiPickerView() {
        emojiPickerItems = buildEmojiPickerItems()

        val bodyLayoutManager =
            GridLayoutManager(context, emojiGridColumns, LinearLayoutManager.VERTICAL,/* reverseLayout = */ false).apply {
                    spanSizeLookup =
                        object : GridLayoutManager.SpanSizeLookup() {
                            override fun getSpanSize(position: Int): Int {
                                return when (emojiPickerItems.getBodyItem(position).itemType) {
                                    ItemType.CATEGORY_TITLE,
                                    ItemType.PLACEHOLDER_TEXT -> emojiGridColumns
                                    else -> 1
                                }
                            }
                        }
                }
        headerAdapter =
            EmojiPickerHeaderAdapter(context, emojiPickerItems, onHeaderIconClicked =
            { with(emojiPickerItems.firstItemPositionByGroupIndex(it)) {
                    if (this == emojiPickerItems.groupRange(recentItemGroup).first) {
                            scope.launch { refreshRecent() }
                    }
                    bodyLayoutManager.scrollToPositionWithOffset(this, 0)
                    // The scroll position change will not be reflected until the next layout
                    // call,
                    // so force a new layout call here.
                    invalidate()
                    }
                }
            )



        // clear view's children in case of resetting layout
        super.removeAllViews()
        with(inflate(context, R.layout.emoji_picker, this)) {
            // set headerView
            ViewCompat.requireViewById<RecyclerView>(this, R.id.emoji_picker_header).apply {
                if(usedInSearchResult){
                    visibility = GONE
                }

                layoutManager = object : LinearLayoutManager(context, HORIZONTAL, /* reverseLayout= */ false) {
                        override fun checkLayoutParams(lp: RecyclerView.LayoutParams): Boolean {
                            lp.width = (width - paddingStart - paddingEnd) / emojiPickerItems.numGroups
                            return true
                        }
                    }
                adapter = headerAdapter
            }

            // set bodyView
            ViewCompat.requireViewById<RecyclerView>(this, R.id.emoji_picker_body).apply {

                layoutManager = bodyLayoutManager
                adapter = createEmojiPickerBodyAdapter()
                        .apply { setHasStableIds(true) }
                        .also { bodyAdapter = it }

                if(!usedInSearchResult){
                    addOnScrollListener(object : RecyclerView.OnScrollListener() {
                        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                            super.onScrolled(recyclerView, dx, dy)
                            headerAdapter.selectedGroupIndex = emojiPickerItems.groupIndexByItemPosition(
                                bodyLayoutManager.findFirstCompletelyVisibleItemPosition()
                            )
                            if (recentNeedsRefreshing &&
                                bodyLayoutManager.findFirstVisibleItemPosition() !in emojiPickerItems.groupRange(recentItemGroup)
                            ) {
                                scope.launch { refreshRecent() }
                            }
                        }
                    }
                    )
                }

                // Disable item insertion/deletion animation. This keeps view holder unchanged when
                // item updates.
                itemAnimator = null
                setRecycledViewPool(
                    RecyclerView.RecycledViewPool().apply {
                        setMaxRecycledViews(
                            ItemType.EMOJI.ordinal,
                            EmojiPickerConstants.EMOJI_VIEW_POOL_SIZE
                        )
                    }
                )
            }
        }
    }

    internal suspend fun refreshRecent() {
        if (!recentNeedsRefreshing || recentEmojiProvider == null) {
            return
        }
        val oldGroupSize = if (::recentItemGroup.isInitialized) recentItemGroup.size else 0
        val recent = recentEmojiProvider?.getRecentEmojiList()
        withContext(Dispatchers.Main) {
            recentItems.clear()
            if (recent != null) {
                recentItems.addAll(recent.map {
                    EmojiViewData(
                        it,
                        updateToSticky = false,
                    )
                })
            }
            if (::emojiPickerItems.isInitialized) {
                if(:: recentItemGroup.isInitialized){
                    val range = emojiPickerItems.groupRange(recentItemGroup)
                    if (recentItemGroup.size > oldGroupSize) {
                        bodyAdapter.notifyItemRangeInserted(
                            range.first + oldGroupSize,
                            recentItemGroup.size - oldGroupSize
                        )
                    } else if (recentItemGroup.size < oldGroupSize) {
                        bodyAdapter.notifyItemRangeRemoved(
                            range.first + recentItemGroup.size,
                            oldGroupSize - recentItemGroup.size
                        )
                    }
                    bodyAdapter.notifyItemRangeChanged(
                        range.first,
                        minOf(oldGroupSize, recentItemGroup.size)
                    )
                    recentNeedsRefreshing = false
                }

            }
        }
    }

    /**
     * This function is used to set the custom behavior after clicking on an emoji icon. Clients
     * could specify their own behavior inside this function.
     */
    fun setOnEmojiPickedListener(onEmojiPickedListener: Consumer<EmojiViewItem>?) {
        this.onEmojiPickedListener = onEmojiPickedListener
    }

    fun setRecentEmojiProvider(recentEmojiProvider: RecentEmojiProvider) {
        this.recentEmojiProvider = recentEmojiProvider
        scope.launch {
            recentNeedsRefreshing = true
            refreshRecent()
        }
    }

    /**
     * The following functions disallow clients to add view to the EmojiPickerView
     *
     * @param child the child view to be added
     * @throws UnsupportedOperationException
     */
    override fun addView(child: View?) {
        if (childCount > 0)
            throw UnsupportedOperationException(EmojiPickerConstants.ADD_VIEW_EXCEPTION_MESSAGE)
        else super.addView(child)
    }

    /**
     * @param child
     * @param params
     * @throws UnsupportedOperationException
     */
//    override fun addView(child: View?, params: LayoutParams?) {
//        if (childCount > 0)
//            throw UnsupportedOperationException(EmojiPickerConstants.ADD_VIEW_EXCEPTION_MESSAGE)
//        else super.addView(child, params)
//    }

    /**
     * @param child
     * @param index
     * @throws UnsupportedOperationException
     */
    override fun addView(child: View?, index: Int) {
        if (childCount > 0)
            throw UnsupportedOperationException(EmojiPickerConstants.ADD_VIEW_EXCEPTION_MESSAGE)
        else super.addView(child, index)
    }

    /**
     * @param child
     * @param index
     * @param params
     * @throws UnsupportedOperationException
     */
//    override fun addView(child: View?, index: Int, params: LayoutParams?) {
//        if (childCount > 0)
//            throw UnsupportedOperationException(EmojiPickerConstants.ADD_VIEW_EXCEPTION_MESSAGE)
//        else super.addView(child, index, params)
//    }

    /**
     * @param child
     * @param width
     * @param height
     * @throws UnsupportedOperationException
     */
    override fun addView(child: View?, width: Int, height: Int) {
        if (childCount > 0)
            throw UnsupportedOperationException(EmojiPickerConstants.ADD_VIEW_EXCEPTION_MESSAGE)
        else super.addView(child, width, height)
    }

    /**
     * The following functions disallow clients to remove view from the EmojiPickerView
     *
     * @throws UnsupportedOperationException
     */
    override fun removeAllViews() {
        throw UnsupportedOperationException(EmojiPickerConstants.REMOVE_VIEW_EXCEPTION_MESSAGE)
    }

    /**
     * @param child
     * @throws UnsupportedOperationException
     */
    override fun removeView(child: View?) {
        throw UnsupportedOperationException(EmojiPickerConstants.REMOVE_VIEW_EXCEPTION_MESSAGE)
    }

    /**
     * @param index
     * @throws UnsupportedOperationException
     */
    override fun removeViewAt(index: Int) {
        throw UnsupportedOperationException(EmojiPickerConstants.REMOVE_VIEW_EXCEPTION_MESSAGE)
    }

    /**
     * @param child
     * @throws UnsupportedOperationException
     */
    override fun removeViewInLayout(child: View?) {
        throw UnsupportedOperationException(EmojiPickerConstants.REMOVE_VIEW_EXCEPTION_MESSAGE)
    }

    /**
     * @param start
     * @param count
     * @throws UnsupportedOperationException
     */
    override fun removeViews(start: Int, count: Int) {
        throw UnsupportedOperationException(EmojiPickerConstants.REMOVE_VIEW_EXCEPTION_MESSAGE)
    }

    /**
     * @param start
     * @param count
     * @throws UnsupportedOperationException
     */
    override fun removeViewsInLayout(start: Int, count: Int) {
        throw UnsupportedOperationException(EmojiPickerConstants.REMOVE_VIEW_EXCEPTION_MESSAGE)
    }
}
