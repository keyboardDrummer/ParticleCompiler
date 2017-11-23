package deltas.bytecode.coreInstructions.doubles

import core.deltas.node.{Node, NodeClass}
import core.deltas.{Compilation, Contract}
import deltas.bytecode.PrintByteCode
import deltas.bytecode.attributes.CodeAttribute
import deltas.bytecode.attributes.CodeAttribute.JumpBehavior
import deltas.bytecode.coreInstructions.{InstructionDelta, InstructionSignature}
import deltas.bytecode.simpleBytecode.ProgramTypeState
import deltas.bytecode.types.DoubleTypeC

object DoubleReturnInstructionDelta extends InstructionDelta {

  override val key = DoubleReturn

  def create: Node = CodeAttribute.instruction(DoubleReturn)

  override def jumpBehavior: JumpBehavior = new JumpBehavior(false, false)

  override def getInstructionSize: Int = 1

  override def getSignature(instruction: Node, typeState: ProgramTypeState, state: Compilation): InstructionSignature =
    InstructionSignature(Seq(DoubleTypeC.doubleType), Seq())

  override def getInstructionByteCode(instruction: Node): Seq[Byte] = PrintByteCode.hexToBytes("af")

  object DoubleReturn extends NodeClass

  override def dependencies: Set[Contract] = super.dependencies ++ Set(DoubleTypeC)

  override def description: String = "Defines the double return instruction, which returns a double from the current method."

  override def grammarName = "dreturn"
}