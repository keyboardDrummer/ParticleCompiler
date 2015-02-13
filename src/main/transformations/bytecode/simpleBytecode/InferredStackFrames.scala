package transformations.bytecode.simpleBytecode

import core.transformation.sillyCodePieces.ParticleWithPhase
import core.transformation.{Contract, MetaObject, TransformationState}
import transformations.bytecode.additions.LabelledTargets
import transformations.bytecode.attributes.{CodeAttribute, StackMapTableAttribute}
import StackMapTableAttribute.{FullFrameStack, FullFrameLocals}
import transformations.bytecode.ByteCodeSkeleton
import transformations.javac.classes.ConstantPool
import transformations.types.TypeC

object InferredStackFrames extends ParticleWithPhase {
  val initialStack = Seq[MetaObject]()

  override def dependencies: Set[Contract] = Set(LabelledTargets)

  def label(name: String) = new MetaObject(LabelledTargets.LabelKey) {
    data.put(LabelledTargets.LabelNameKey, name)
  }

  override def transform(program: MetaObject, state: TransformationState): Unit = {
    val clazz = program
    val constantPool = ByteCodeSkeleton.getConstantPool(clazz)
    for (method <- ByteCodeSkeleton.getMethods(clazz)) {
      val methodDescriptor = constantPool.getValue(ByteCodeSkeleton.getMethodDescriptorIndex(method)).asInstanceOf[MetaObject]
      val initialLocals = ByteCodeSkeleton.getMethodDescriptorParameters(methodDescriptor)
      val codeAnnotation = ByteCodeSkeleton.getMethodAttributes(method).find(a => a.clazz == CodeAttribute.CodeKey).get
      val instructions = CodeAttribute.getCodeInstructions(codeAnnotation)

      val stackLayouts: Map[Int, Seq[MetaObject]] = getStackLayoutsPerInstruction(state, instructions)
      val localTypes: Map[Int, Map[Int, MetaObject]] = getLocalTypes(initialLocals, instructions)
      localTypes.size
      var previousStack = initialStack
      var previousLocals = initialLocals
      for (indexedLabel <- instructions.zipWithIndex.filter(i => i._1.clazz == LabelledTargets.LabelKey)) {
        val index = indexedLabel._2
        val label = indexedLabel._1
        val currentStack = stackLayouts(index)
        val locals = getLocalTypesSequenceFromMap(localTypes(index))
        label(LabelledTargets.LabelStackFrame) = getStackMap(previousStack, currentStack, previousLocals, locals)
        previousStack = currentStack
        previousLocals = locals
      }
    }

    def getLocalTypesSequenceFromMap(localTypes: Map[Int, MetaObject]): Seq[MetaObject] = {
      val max = localTypes.keys.max
      0.to(max).map(index => localTypes.getOrElse(index, throw new NotImplementedError))
    }

    def getLocalTypes(initialLocals: Seq[MetaObject], instructions: Seq[MetaObject]): Map[Int, Map[Int, MetaObject]] = {
      val instructionVariableUpdateRegistry = ByteCodeSkeleton.getState(state).localUpdates
      val analysis: LocalTypeAnalysis = new LocalTypeAnalysis(instructions, instruction => instructionVariableUpdateRegistry(instruction.clazz)(instruction), state)
      analysis.run(0, initialLocals.zipWithIndex.map(p => p._2 -> p._1).toMap)
    }

    def getStackLayoutsPerInstruction(state: TransformationState, instructions: Seq[MetaObject]): Map[Int, Seq[MetaObject]] = {
      val instructionSignatureRegistry = ByteCodeSkeleton.getInstructionSignatureRegistry(state)
      val stackAnalysis: StackLayoutAnalysis = new StackLayoutAnalysis(instructions,
        instruction => instructionSignatureRegistry(instruction.clazz)(constantPool, instruction)._1,
        instruction => instructionSignatureRegistry(instruction.clazz)(constantPool, instruction)._2,
        state)
      stackAnalysis.run(0, initialStack)
    }

    def toStackType(_type: MetaObject) = TypeC.toStackType(constantPool, _type)

    def getStackMap(previousStack: Seq[MetaObject], stack: Seq[MetaObject], previousLocals: Seq[MetaObject], locals: Seq[MetaObject]) = {
      getStackMapHelper(previousStack.map(toStackType), stack.map(toStackType), previousLocals.map(toStackType), locals.map(toStackType))
    }

    def getStackMapHelper(previousStack: Seq[MetaObject], stack: Seq[MetaObject], previousLocals: Seq[MetaObject], locals: Seq[MetaObject]) = {
      val sameLocalsPrefix = previousLocals.zip(locals).filter(p => p._1 == p._2)
      val removedLocals = previousLocals.drop(sameLocalsPrefix.length)
      val addedLocals = locals.drop(sameLocalsPrefix.length)
      val unchangedLocals = removedLocals.isEmpty && addedLocals.isEmpty
      if (unchangedLocals && stack.isEmpty) {
        new MetaObject(StackMapTableAttribute.SameFrameKey)
      }
      else if (unchangedLocals && stack.size == 1) {
        new MetaObject(StackMapTableAttribute.SameLocals1StackItem) {
          data.put(StackMapTableAttribute.SameLocals1StackItemType, stack(0))
        }
      }
      else if (stack.isEmpty && addedLocals.isEmpty) {
        new MetaObject(StackMapTableAttribute.ChopFrame) {
          data.put(StackMapTableAttribute.ChopFrameCount, removedLocals.length)
        }
      }
      else if (stack.isEmpty && removedLocals.isEmpty) {
        new MetaObject(StackMapTableAttribute.AppendFrame) {
          data.put(StackMapTableAttribute.AppendFrameTypes, addedLocals.map(toStackType))
        }
      }
      else {
        new MetaObject(StackMapTableAttribute.FullFrame, FullFrameLocals -> locals, FullFrameStack -> stack)
      }

    }
  }


}
