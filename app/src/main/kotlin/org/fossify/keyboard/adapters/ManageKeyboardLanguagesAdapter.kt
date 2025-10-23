package org.fossify.keyboard.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.fossify.commons.views.MyAppCompatCheckbox
import org.fossify.keyboard.R
import org.fossify.keyboard.helpers.Config

typealias LanguageItem = Pair<Int, String>

internal class ManageKeyboardLanguagesAdapter(
    private val config: Config,
    private var languagesList: List<LanguageItem>,
) : RecyclerView.Adapter<ManageKeyboardLanguagesAdapter.MyViewHolder>() {
    private val selectedLanguages = config.selectedLanguages

    internal inner class MyViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var languageCheckboxItem: MyAppCompatCheckbox = view.findViewById(R.id.language_checkbox)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.language_checkbox_item, parent, false)
        return MyViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val item = languagesList[position]
        holder.languageCheckboxItem.apply {
            text = item.second
            isChecked = selectedLanguages.contains(item.first)

            setOnClickListener {
                if (isChecked) {
                    selectedLanguages.add(item.first)
                } else {
                    selectedLanguages.remove(item.first)
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return languagesList.size
    }

    fun getSelectedLanguages(): MutableSet<Int> {
        val defaultLang = config.getDefaultLanguage()
        if (selectedLanguages.isEmpty()) {
            selectedLanguages.add(defaultLang)
        }
        return selectedLanguages
    }
}
