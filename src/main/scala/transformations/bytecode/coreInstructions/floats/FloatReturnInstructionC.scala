package transformations.bytecode.coreInstructions.floats

import core.particles.node.{Key, Node}
import core.particles.{CompilationState, Contract}
import transformations.bytecode.PrintByteCode
import transformations.bytecode.attributes.CodeAttribute
import transformations.bytecode.attributes.CodeAttribute.JumpBehavior
import transformations.bytecode.coreInstructions.{InstructionC, InstructionSignature}
import transformations.bytecode.simpleBytecode.ProgramTypeState
import transformations.bytecode.types.{DoubleTypeC, FloatTypeC}

object FloatReturnInstructionC extends InstructionC {

  override val key: Key = FloatReturn

  def create: Node = CodeAttribute.instruction(FloatReturn)

  override def jumpBehavior: JumpBehavior = new JumpBehavior(false, false)

  override def getInstructionSize: Int = 1

  override def getSignature(instruction: Node, typeState: ProgramTypeState, state: CompilationState): InstructionSignature =
    InstructionSignature(Seq(FloatTypeC.floatType), Seq())

  override def getInstructionByteCode(instruction: Node): Seq[Byte] = PrintByteCode.hexToBytes("ae")

  object FloatReturn extends Key

  override def dependencies: Set[Contract] = super.dependencies ++ Set(FloatTypeC)

  override def description: String = "Defines the float return instruction, which returns a float from the current method."
}
