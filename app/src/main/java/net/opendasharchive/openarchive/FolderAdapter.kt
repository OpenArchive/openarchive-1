package net.opendasharchive.openarchive

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import net.opendasharchive.openarchive.databinding.RvFoldersRowBinding
import net.opendasharchive.openarchive.db.Project
import java.lang.ref.WeakReference

interface FolderAdapterListener {

    fun projectClicked(project: Project)

    fun projectEdit(project: Project)

    fun getSelectedProject(): Project?
}

class FolderAdapter(
    private val context: Context,
    listener: FolderAdapterListener?,
    val isArchived: Boolean = false
) : ListAdapter<Project, FolderAdapter.ViewHolder>(DIFF_CALLBACK), FolderAdapterListener {

    inner class ViewHolder(private val binding: RvFoldersRowBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(listener: WeakReference<FolderAdapterListener>?, project: Project?) {

            val isSelected = listener?.get()?.getSelectedProject()?.id == project?.id
            itemView.isSelected = isSelected

            val textColorRes = if (isSelected) R.color.colorTertiary else R.color.colorText
            val iconColorRes = if (isSelected) R.color.colorTertiary else R.color.colorOnBackground
            val backgroundRes = if (isSelected) R.drawable.item_background_selector else android.R.color.transparent

            binding.root.setBackgroundResource(backgroundRes)

            binding.rvTitle.text = project?.description
            binding.rvTitle.setTextColor(ContextCompat.getColor(context, textColorRes))

            val icon = if (isSelected) {
                ContextCompat.getDrawable(context, R.drawable.baseline_folder_white_24)
            } else {
                ContextCompat.getDrawable(context, R.drawable.outline_folder_white_24)
            }

            icon?.setTint(ContextCompat.getColor(context, iconColorRes))

            binding.rvIcon.setImageDrawable(icon)

            if (isArchived) {
                binding.rvEdit.visibility = View.GONE
            } else {
                binding.rvEdit.visibility = View.VISIBLE
            }



            if (project != null) {
                binding.textContainer.setOnClickListener {
                    if (isArchived) {
                        listener?.get()?.projectEdit(project)
                    } else {
                        listener?.get()?.projectClicked(project)
                    }
                }

                binding.rvEdit.setOnClickListener {
                    listener?.get()?.projectEdit(project)
                }

            } else {
                binding.root.setOnClickListener(null)
            }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Project>() {
            override fun areItemsTheSame(oldItem: Project, newItem: Project): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: Project, newItem: Project): Boolean {
                return oldItem.description == newItem.description
            }
        }

        private var highlightColor: Int? = null
        private var defaultColor: Int? = null

        fun getColorOld(context: Context, highlight: Boolean): Int {
            if (highlight) {
                var color = highlightColor

                if (color != null) return color

                color = ContextCompat.getColor(context, R.color.colorPrimary)
                highlightColor = color

                return color
            }

            var color = defaultColor

            if (color != null) return color

            val textview = TextView(context)
            color = textview.currentTextColor
            defaultColor = color

            return color
        }

        fun getColor(context: Context, highlight: Boolean): Int {
            return if (highlight) {
                ContextCompat.getColor(context, R.color.colorPrimary)
            } else {
                ContextCompat.getColor(context, R.color.colorOnBackground)
            }
        }
    }

    private val mListener: WeakReference<FolderAdapterListener>? = WeakReference(listener)

    private var mLastSelected: Project? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            RvFoldersRowBinding.inflate(
                LayoutInflater.from(parent.context),
                parent, false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val project = getItem(position)

        holder.bind(WeakReference(this), project)
    }

    fun update(projects: List<Project>) {
        notifyItemChanged(getIndex(mLastSelected))

        submitList(projects)
    }

    override fun projectClicked(project: Project) {
        notifyItemChanged(getIndex(getSelectedProject()))
        notifyItemChanged(getIndex(project))

        mListener?.get()?.projectClicked(project)
    }

    override fun getSelectedProject(): Project? {
        mLastSelected = mListener?.get()?.getSelectedProject()

        return mLastSelected
    }

    override fun projectEdit(project: Project) {
        notifyItemChanged(getIndex(getSelectedProject()))
        notifyItemChanged(getIndex(project))

        mListener?.get()?.projectEdit(project)
    }

    private fun getIndex(project: Project?): Int {
        return if (project == null) {
            -1
        } else {
            currentList.indexOf(project)
        }
    }
}