package camp.nextstep.edu.calculator

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import camp.nextstep.edu.calculator.domain.Operator
import camp.nextstep.edu.calculator.domain.model.CalculatorResultData
import camp.nextstep.edu.calculator.domain.usecase.GetCalculatorResultUseCase
import camp.nextstep.edu.calculator.domain.usecase.SaveCalculatorResultUseCase
import camp.nextstep.edu.calculator.local.db.CalculatorDatabase
import camp.nextstep.edu.calculator.local.di.InjectDatabase.getDB
import camp.nextstep.edu.calculator.local.di.InjectRepositoryImpl
import com.google.common.truth.Truth
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.Executors

@RunWith(RobolectricTestRunner::class)
class CalculatorViewModelTest {
    private lateinit var viewModel: CalculatorViewModel

    private lateinit var db : CalculatorDatabase

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        db = getDB(context)!!
        val repository =
            InjectRepositoryImpl.repositoryImpl(context, Executors.newSingleThreadExecutor())

        val get = GetCalculatorResultUseCase(repository)
        val save = SaveCalculatorResultUseCase(repository)
        viewModel = CalculatorViewModel(get, save)
    }

    @After
    fun tearDown() {
        Executors.newSingleThreadExecutor().submit {
            db.clearAllTables()
        }
        db.close()
    }

    @Test
    fun `입력된 피연산자가 없을 때, 사용자가 피연산자 0 ~ 9 입력시 해당 숫자가 입력되어야 한다`() {
        // when
        (0..9).forEach {
            viewModel.addToExpression(it)
        }

        //then
        val actual = viewModel.calcText.getOrAwaitValue()
        Truth.assertThat(actual).isEqualTo("123456789")
    }

    @Test
    fun `입력된 피연산자가 있을 때, 숫자 입력 시 기존의 피연산자 뒤에 숫자가 입력되어야 한다`() {
        // 8 -> 9 입력 -> 89

        // given
        viewModel.addToExpression(8)

        // when
        viewModel.addToExpression(9)

        //then
        val actual = viewModel.calcText.getOrAwaitValue()
        Truth.assertThat(actual).isEqualTo("89")
    }

    @Test
    fun `입력된 피연산자가 없을 때, 사용자가 연산자 +, -, ×, ÷ 입력을 하면 아무런 변화가 없어야 한다`() {
        // EMPTY -> EMPTY

        // when
        viewModel.addToExpression(Operator.Plus)

        //then
        val actual = viewModel.calcText.getOrAwaitValue()
        Truth.assertThat(actual).isEqualTo("")
    }

    @Test
    fun `입력된 피연산자가 있을 때, 연산자 +, -, ×, ÷ 입력을 하면 입력되어야 한다`() {
        /*
        * 1 -> '+ 입력' -> 1 +
        * 1 + -> '- 입력' -> 1 -
        * */
        // given
        viewModel.addToExpression(1)

        // when
        viewModel.addToExpression(Operator.Plus)
        //then
        val plusActual = viewModel.calcText.getOrAwaitValue()
        Truth.assertThat(plusActual).isEqualTo("1 +")

        // when
        viewModel.addToExpression(Operator.Minus)
        //then
        val minusActual = viewModel.calcText.getOrAwaitValue()
        Truth.assertThat(minusActual).isEqualTo("1 -")

    }

    @Test
    fun `입력된 수식이 없을 때, 수식을 지우는 경우 변화가 없어야 한다`() {
        // EMPTY -> EMPTY

        // when
        viewModel.removeLast()

        //then
        val actual = viewModel.calcText.getOrAwaitValue()
        Truth.assertThat(actual).isEqualTo("")
    }

    @Test
    fun `입력된 수식이 있을 때, 사용자가 지우기를 하면 수식에 마지막으로 입력된 연산자 또는 피연산자가 지워져야 한다`() {
        /*
        * 32 + 1 -> 지우기
        * 32 +  -> 지우기
        * 32 -> 지우기
        * 3 -> 지우기
        * EMPTY -> 지우기
        * EMPTY
        */

        // given
        viewModel.addToExpression(3)
        viewModel.addToExpression(2)
        viewModel.addToExpression(Operator.Plus)
        viewModel.addToExpression(1)

        // when:
        viewModel.removeLast()
        //then:
        val actual1 = viewModel.calcText.getOrAwaitValue()
        Truth.assertThat(actual1).isEqualTo("32 +")

        // when:
        viewModel.removeLast()
        //then:
        val actual2 = viewModel.calcText.getOrAwaitValue()
        Truth.assertThat(actual2).isEqualTo("32")

        // when:
        viewModel.removeLast()
        //then:
        val actual3 = viewModel.calcText.getOrAwaitValue()
        Truth.assertThat(actual3).isEqualTo("3")

        // when:
        viewModel.removeLast()
        //then:
        val actual4 = viewModel.calcText.getOrAwaitValue()
        Truth.assertThat(actual4).isEqualTo("")

        // when:
        viewModel.removeLast()
        //then:
        val actual5 = viewModel.calcText.getOrAwaitValue()
        Truth.assertThat(actual5).isEqualTo("")
    }

    @Test
    fun `입력된 수신이 완전할 때, 사용자가 = 버튼을 누르면 입력된 수식의 결과가 화면에 보여야 한다`() {
        //3 + 2 -> 계산 결과 -> 5

        // given
        viewModel.addToExpression(3)
        viewModel.addToExpression(Operator.Plus)
        viewModel.addToExpression(2)

        // when:
        viewModel.calculate()

        //then:
        val actual = viewModel.calcText.getOrAwaitValue()
        Truth.assertThat(actual).isEqualTo("5")
    }

    @Test
    fun `입력된 수식이 완전하지 않을 때, 계산하는 경우 에러 메세지를 보여줘야 한다`() {
        // given: '3 +'
        viewModel.addToExpression(3)
        viewModel.addToExpression(Operator.Plus)

        // when: 계산결과
        viewModel.calculate()

        //then: 완성되지 않은 수식입니다 출력
        val actual = viewModel.calculatorErrorMessage.getOrAwaitValue()
        Truth.assertThat(actual).isEqualTo(R.string.incomplete_expression)
    }

    @Test
    fun `계산기록 보기의 초기값은 false여야 한다`() {
        // when
        val actual = viewModel.isCalculatorResultShow.getOrAwaitValue()

        //then
        Truth.assertThat(actual).isEqualTo(false)
    }

    @Test
    fun `계산기록 보기함수 호출 후 값은 기존의 값에서 not한 상태여야한다`() {
        // when
        val expected = viewModel.isCalculatorResultShow.getOrAwaitValue()
        viewModel.loadResultList()

        //then
        val actual = viewModel.isCalculatorResultShow.getOrAwaitValue()
        Truth.assertThat(actual).isEqualTo(expected?.not())
    }

    @Test
    fun `초기 계산기록의 값은 빈 리스트여야 한다`() {
        // when
        viewModel.loadResultList()

        //then
        val actual = viewModel.calculatorResultList.getOrAwaitValue()
        Truth.assertThat(actual).isEqualTo(emptyList<CalculatorResultData>())
    }

    @Test
    fun `1 + 1 입력된 상태에서 calculator() 호출 후 계산기록 리스트의 값이 존재해야 한다`() {
        // given
        viewModel.addToExpression(1)
        viewModel.addToExpression(Operator.Plus)
        viewModel.addToExpression(1)
        viewModel.calculate()

        // when
        viewModel.loadResultList()

        //then
        val actual = viewModel.calculatorResultList.getOrAwaitValue()?.get(0)
        Truth.assertThat(actual).isEqualTo(CalculatorResultData("1 + 1", 2))
    }
}