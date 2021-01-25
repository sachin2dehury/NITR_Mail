package github.sachin2dehury.owlmail.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.core.view.isVisible
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import github.sachin2dehury.owlmail.R
import github.sachin2dehury.owlmail.api.data.Mail
import github.sachin2dehury.owlmail.databinding.MailItemBinding
import github.sachin2dehury.owlmail.others.Constants
import java.text.SimpleDateFormat

class MailBoxAdapter(context: Context) :
    RecyclerView.Adapter<MailBoxAdapter.MailBoxViewHolder>(), Filterable {

    class MailBoxViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    private val diffCallback = object : DiffUtil.ItemCallback<Mail>() {

        override fun areItemsTheSame(oldItem: Mail, newItem: Mail): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Mail, newItem: Mail): Boolean {
            return oldItem.hashCode() == newItem.hashCode()
        }
    }

    private var onItemClickListener: ((Mail) -> Unit)? = null

    private val differ = AsyncListDiffer(this, diffCallback)

//    private val colors = context.resources.getIntArray(R.array.colors)
//    private val colorsLength = colors.size

    var list: List<Mail> = emptyList()

    var mails: List<Mail>
        get() = differ.currentList
        set(value) = differ.submitList(value)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MailBoxViewHolder {
        return MailBoxViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.mail_item, parent, false)
        )
    }

    override fun onBindViewHolder(holder: MailBoxViewHolder, position: Int) {
        val mail = mails[position]
        val binding = MailItemBinding.bind(holder.itemView)
        val sender =
            if (mail.flag.contains('s')) mail.addresses.first() else mail.addresses.last()

        @SuppressLint("SimpleDateFormat")
        val dateFormat = when {
            (System.currentTimeMillis() - mail.time) < Constants.DAY -> {
                SimpleDateFormat(Constants.DATE_FORMAT_DATE)
            }
            (System.currentTimeMillis() - mail.time) < Constants.YEAR -> {
                SimpleDateFormat(Constants.DATE_FORMAT_MONTH)
            }
            else -> {
                SimpleDateFormat(Constants.DATE_FORMAT_YEAR)
            }
        }
        val name = if (sender.name.isNotEmpty()) sender.name else sender.email.substringBefore('@')
//        val color = colors[mail.size % colorsLength]
        val color = when (sender.email.contains("@nitrkl.ac.in", true)) {
            true -> {
                try {
                    sender.email.first().toInt()
                    R.color.rally_green_300
                } catch (e: Exception) {
                    R.color.rally_green_500
                }
            }
            else -> R.color.rally_green_700
        }
        binding.apply {
            textViewSender.text = name.first().toString()
            imageViewSender.setColorFilter(color)
            textViewDate.text = dateFormat.format(mail.time)
            textViewMailBody.text = mail.body
            textViewMailSubject.text = mail.subject
            textViewSenderEmail.text = name
            if (mail.flag.contains('u')) {
                textViewSenderEmail.typeface = Typeface.DEFAULT_BOLD
                textViewMailSubject.typeface = Typeface.DEFAULT_BOLD
                textViewDate.typeface = Typeface.DEFAULT_BOLD
                textViewMailBody.typeface = Typeface.DEFAULT_BOLD
            }
            if (mail.flag.contains('f')) {
                imageViewStared.isVisible = true
            }
            if (mail.flag.contains('a')) {
                imageViewAttachment.isVisible = true
            }
//            if (mail.flag.contains('r')) {
//                imageViewReply.isVisible = true
//            }
            if (position == mails.lastIndex) {
                divider.isVisible = false
            }
        }
        holder.itemView.setOnClickListener {
            onItemClickListener?.let { click ->
                click(mail)
            }
        }
    }

    override fun getItemCount(): Int {
        return mails.size
    }

    fun setOnItemClickListener(onItemClick: (Mail) -> Unit) {
        this.onItemClickListener = onItemClick
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(value: CharSequence?): FilterResults {
                val search = value.toString()
                val filterResults = FilterResults()
                filterResults.values = if (search.isEmpty()) {
                    list
                } else {
                    list.filter { mail ->
                        (if (mail.flag.contains('s')) mail.addresses.first().email else mail.addresses.last().email).contains(
                            search,
                            true
                        )
                                || mail.body.contains(search, true)
                                || mail.subject.contains(search, true)
                                || mail.parsedBody.contains(search, true)
                    }
                }
                return filterResults
            }

            @Suppress("UNCHECKED_CAST")
            override fun publishResults(value: CharSequence?, filterResults: FilterResults?) {
                mails = filterResults?.values as List<Mail>
            }
        }
    }
}