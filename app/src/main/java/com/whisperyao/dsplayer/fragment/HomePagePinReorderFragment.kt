package com.whisperyao.dsplayer.fragment


import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.whisperyao.dsplayer.R
import com.whisperyao.dsplayer.adapters.AbsAdapter
import com.whisperyao.dsplayer.adapters.PinReOrderAdapter
import com.whisperyao.dsplayer.adapters.SimpleItemTouchHelperCallback
import com.whisperyao.dsplayer.databinding.FragmentPinReorderBinding
import com.whisperyao.dsplayer.databinding.PinEditSpinnerItemBinding
import com.whisperyao.dsplayer.homepage.PinManager
import java.util.ArrayList

class HomePagePinReorderFragment : DialogFragment() {

    private lateinit var binding: FragmentPinReorderBinding
    private lateinit var pinReOrderAdapter: PinReOrderAdapter
    private lateinit var selectModeAdapter: SelectModeAdapter

    private var reorderDirty = false
    private var selectedItemSum = 0

    private val deleteIdList = arrayListOf<String>()
    private val reorderIdList = arrayListOf<String>()

    private val callback = object : AbsAdapter.Callback {
        override fun onItemSelected(count: Int) {
            selectedItemSum = count
            selectModeAdapter.notifyDataSetChanged()
            updateBottomMenu()
        }

        override fun onTrackOrderChanged(playingPos: Int) {
            reorderDirty = true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, androidx.appcompat.R.style.Theme_AppCompat_Light_DialogWhenLarge)

        val items = PinManager.getInstance().getItems()
        val copied = items.map { it.copy() }

        pinReOrderAdapter = PinReOrderAdapter(callback).apply {
            setData(ArrayList(copied))
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentPinReorderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupToolbar()
        setupSpinner()
        setupRecyclerView()
        setupBottomMenu()
    }

    private fun setupToolbar() {
        binding.toolbar.apply {
            setNavigationOnClickListener { dismiss() }
            inflateMenu(R.menu.pin_edit_toolbar_menu)
            setOnMenuItemClickListener { onMenuItemClick(it) }
        }
    }

    private fun setupSpinner() {
        val context = requireContext()

        val options = arrayOf(
            getString(R.string.select_all),
            getString(R.string.deselect_all)
        )

        selectModeAdapter = SelectModeAdapter(context, R.layout.action_mode_spinner_item, options)

        binding.actionModeSpinner.spinner.apply {
            adapter = selectModeAdapter
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: AdapterView<*>?) {}

                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    when (position) {
                        0 -> markAllItem(true)
                        1 -> markAllItem(false)
                    }
                    selectModeAdapter.notifyDataSetChanged()
                }
            }
        }
    }

    private fun setupRecyclerView() {
        val recyclerView = binding.recyclerView

        val touchHelper = ItemTouchHelper(
            SimpleItemTouchHelperCallback(pinReOrderAdapter)
        )

        touchHelper.attachToRecyclerView(recyclerView)

        pinReOrderAdapter.apply {
            setTouchHelper(touchHelper)
            setIsDragMode(true)
            setIsCheckMode(true)
            setOnItemClickListener { _, _, _ -> }
        }

        recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            post { adapter = pinReOrderAdapter }
        }
    }

    private fun setupBottomMenu() {
        val menu = binding.actionMenu.menu
        requireActivity().menuInflater.inflate(R.menu.pin_edit_action_menu, menu)

        binding.actionMenu.setOnMenuItemClickListener {
            onMenuItemClick(it)
        }
    }

    private fun onMenuItemClick(item: MenuItem): Boolean {
        return when (item.itemId) {

            R.id.pin_edit_menu_ok -> {
                handleConfirm()
                false
            }

            R.id.pin_edit_menu_unpin -> {
                handleUnpin()
                false
            }

            else -> false
        }
    }

    private fun handleConfirm() {
        if (reorderDirty) {
            val reordered = pinReOrderAdapter.reOrdedSet

            reorderIdList.clear()
            reorderIdList.addAll(reordered.map { it.id })

            if (deleteIdList.isNotEmpty()) {
                PinManager.getInstance().unpinAndReorder(deleteIdList, reorderIdList)
            } else {
                PinManager.getInstance().reorder(reorderIdList)
            }

        } else if (deleteIdList.isNotEmpty()) {
            PinManager.getInstance().unpin(deleteIdList)
        }

        dismiss()
    }

    private fun handleUnpin() {
        val removed = pinReOrderAdapter.removeSelectedItem()

        deleteIdList.addAll(removed.map { it.id })

        selectedItemSum = 0
        selectModeAdapter.notifyDataSetChanged()
        updateBottomMenu()
    }

    private fun markAllItem(marked: Boolean) {
        if (marked) {
            pinReOrderAdapter.checkAll()
        } else {
            pinReOrderAdapter.unCheckAll()
        }
        updateBottomMenu()
    }

    private fun updateBottomMenu() {
        binding.actionLayout.visibility =
            if (selectedItemSum > 0) View.VISIBLE else View.GONE
    }

    /**
     * Spinner Adapter
     */
    inner class SelectModeAdapter(context: Context, layout: Int, objects: Array<String>) : ArrayAdapter<String>(context, layout, objects) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val textView = (convertView as? TextView)
                ?: PinEditSpinnerItemBinding
                    .inflate(LayoutInflater.from(context), parent, false)
                    .root

            textView.text = when (selectedItemSum) {
                0 -> getMultiString(0)
                1 -> context.getString(R.string.one_item)
                else -> getMultiString(selectedItemSum)
            }

            return textView
        }

        private fun getMultiString(count: Int): String {
            return context.getString(R.string.multi_items)
                .replace("[__DELETE_COUNT__]", count.toString())
        }
    }
}