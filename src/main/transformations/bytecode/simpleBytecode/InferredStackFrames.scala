package transformations.bytecode.simpleBytecode

import core.transformation.sillyCodePieces.ProgramTransformation
import core.transformation.{Contract, MetaObject, TransformationState}
import transformations.bytecode.ByteCodeSkeleton.{FullFrameLocals, FullFrameStack}
import transformations.bytecode.{ByteCodeSkeleton, LabelledJumps}
import transformations.javac.base.ConstantPool

object InferredStackFrames extends ProgramTransformation {
  override def dependencies: Set[Contract] = Set(LabelledJumps)

  def label(name: String) = new MetaObject(LabelledJumps.LabelKey) {
    data.put(LabelledJumps.LabelNameKey, name)
  }

  val initialStack = Seq[MetaObject]()

  override def transform(program: MetaObject, state: TransformationState): Unit = {
    val clazz = program
    val constantPool = new ConstantPool(ByteCodeSkeleton.getConstantPool(clazz))
    for (method <- ByteCodeSkeleton.getMethods(clazz)) {
      val methodDescriptor = constantPool.getValue(ByteCodeSkeleton.getMethodDescriptorIndex(method)).asInstanceOf[MetaObject]
      val initialLocals = ByteCodeSkeleton.getMethodDescriptorParameters(methodDescriptor)
      val codeAnnotation = ByteCodeSkeleton.getMethodAttributes(method).find(a => a.clazz == ByteCodeSkeleton.CodeKey).get
      val instructions = ByteCodeSkeleton.getCodeInstructions(codeAnnotation)

      val stackLayouts: Map[Int, Seq[MetaObject]] = getStackLayoutsPerInstruction(state, instructions)
      val localTypes: Map[Int, Map[Int, MetaObject]] = getLocalTypes(initialLocals, instructions)
      localTypes.size
      var previousStack = initialStack
      var previousLocals = initialLocals
      for (indexedLabel <- instructions.zipWithIndex.filter(i => i._1.clazz == LabelledJumps.LabelKey)) {
        val index = indexedLabel._2
        val label = indexedLabel._1
        val currentStack = stackLayouts(index)
        val locals = getLocalTypesSequenceFromMap(localTypes(index))
        label(LabelledJumps.LabelStackFrame) = getStackMap(previousStack, currentStack, previousLocals, locals)
        previousStack = currentStack
        previousLocals = locals
      }
    }

    def getLocalTypesSequenceFromMap(localTypes: Map[Int, MetaObject]): Seq[MetaObject] = {
      val max = localTypes.keys.max
      0.until(max).map(index => localTypes.getOrElse(index, throw new NotImplementedError))
    }

    def getLocalTypes(initialLocals: Seq[MetaObject], instructions: Seq[MetaObject]): Map[Int, Map[Int, MetaObject]] = {
      val instructionVariableUpdateRegistry = ByteCodeSkeleton.getState(state).localUpdates
      new LocalTypeAnalysis(instructions,
        instruction => instructionVariableUpdateRegistry(instruction.clazz)(instruction), state).run(0,
          initialLocals.zipWithIndex.map(p => p._2 -> p._1).toMap)
    }

    def getStackLayoutsPerInstruction(state: TransformationState, instructions: Seq[MetaObject]): Map[Int, Seq[MetaObject]] = {
      val instructionSignatureRegistry = ByteCodeSkeleton.getInstructionSignatureRegistry(state)
      val stackAnalysis: StackLayoutAnalysis = new StackLayoutAnalysis(instructions,
        instruction => instructionSignatureRegistry(instruction.clazz)(constantPool, instruction)._1,
        instruction => instructionSignatureRegistry(instruction.clazz)(constantPool, instruction)._2,
        state)
      val currentStacks = stackAnalysis.run(0, initialStack)
      currentStacks
    }
  }

  def getStackMap(previousStack: Seq[Any], stack: Seq[Any], previousLocals: Seq[Any], locals: Seq[Any]) = {
    val sameLocalsPrefix = previousLocals.zip(locals).filter(p => p._1 == p._2)
    val removedLocals = previousLocals.drop(sameLocalsPrefix.length)
    val addedLocals = locals.drop(sameLocalsPrefix.length)
    val unchangedLocals = removedLocals.isEmpty && addedLocals.isEmpty
    if (unchangedLocals && stack.isEmpty) {
      new MetaObject(ByteCodeSkeleton.SameFrameKey)
    }
    else if (unchangedLocals && stack.size == 1) {
      new MetaObject(ByteCodeSkeleton.SameLocals1StackItem) {
        data.put(ByteCodeSkeleton.SameLocals1StackItemType, stack(0))
      }
    }
    else if (stack.isEmpty && addedLocals.isEmpty) {
      new MetaObject(ByteCodeSkeleton.ChopFrame) {
        data.put(ByteCodeSkeleton.ChopFrameCount, removedLocals.length)
      }
    }
    else if (stack.isEmpty && removedLocals.isEmpty) {
      new MetaObject(ByteCodeSkeleton.AppendFrame) {
        data.put(ByteCodeSkeleton.AppendFrameTypes, addedLocals)
      }
    }
    else {
      new MetaObject(ByteCodeSkeleton.FullFrame, FullFrameLocals -> locals, FullFrameStack -> stack)
    }

  }


}