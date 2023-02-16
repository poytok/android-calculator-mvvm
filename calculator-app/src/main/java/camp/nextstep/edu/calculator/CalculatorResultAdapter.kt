package camp.nextstep.edu.calculator

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import camp.nextstep.edu.calculator.CalculatorResultAdapter.CalculatorResultViewHolder
import camp.nextstep.edu.calculator.databinding.ItemResultBinding
import camp.nextstep.edu.calculator.domain.model.CalculatorResultData

class CalculatorResultAdapter : ListAdapter<CalculatorResultData, CalculatorResultViewHolder>(diff) {
    inner class CalculatorResultViewHolder(
        private val binding: ItemResultBinding
        ) : RecyclerView.ViewHolder(binding.root) {
            fun bind(data: CalculatorResultData) {
                binding.data = data
            }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CalculatorResultViewHolder {
        val binding = ItemResultBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CalculatorResultViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CalculatorResultViewHolder, position: Int) {
        holder.bind(currentList[position])
    }

    companion object {
        private val diff = object : DiffUtil.ItemCallback<CalculatorResultData>() {
            override fun areItemsTheSame(
                oldItem: CalculatorResultData,
                newItem: CalculatorResultData,
            ): Boolean = oldItem == newItem

            override fun areContentsTheSame(
                oldItem: CalculatorResultData,
                newItem: CalculatorResultData,
            ): Boolean = oldItem == newItem

        }
    }
}