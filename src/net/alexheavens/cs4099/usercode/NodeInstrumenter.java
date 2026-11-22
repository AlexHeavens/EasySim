package net.alexheavens.cs4099.usercode;

import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassHierarchyResolver;
import java.lang.classfile.ClassModel;
import java.lang.classfile.ClassTransform;
import java.lang.classfile.CodeBuilder;
import java.lang.classfile.CodeElement;
import java.lang.classfile.CodeModel;
import java.lang.classfile.CodeTransform;
import java.lang.classfile.Label;
import java.lang.classfile.MethodTransform;
import java.lang.classfile.instruction.BranchInstruction;
import java.lang.classfile.instruction.ExceptionCatch;
import java.lang.classfile.instruction.LabelTarget;
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;
import java.util.HashSet;
import java.util.Set;

/**
 * Adds a call to {@link KillableThread#check()} at the start of every method, at every loop head (the target of a
 * backward branch) and at the start of every exception handler of compiled user code, so a killed node cannot run
 * on in a loop or a recursion that never blocks. The check at a loop head can sit inside a <code>try</code> whose
 * handler swallows the {@link NodeKilledError}; the check at the handler's start throws it again from outside that
 * <code>try</code>, so the error leaves one level per handler. Without it a thread cannot be stopped from outside on JDK 20 and later.
 */
public final class NodeInstrumenter {

	private static final ClassDesc KILLABLE = ClassDesc.of(KillableThread.class.getName());
	private static final MethodTypeDesc CHECK = MethodTypeDesc.of(ConstantDescs.CD_void);

	private NodeInstrumenter() {
	}

	public static byte[] instrument(byte[] classBytes) {
		ClassFile cf = ClassFile.of(ClassFile.ClassHierarchyResolverOption.of(ClassHierarchyResolver
				.defaultResolver().orElse(ClassHierarchyResolver.ofClassLoading(NodeInstrumenter.class.getClassLoader()))));
		ClassModel model = cf.parse(classBytes);
		MethodTransform methods = (builder, element) -> {
			if (element instanceof CodeModel code)
				builder.transformCode(code, safePoints(code));
			else
				builder.with(element);
		};
		return cf.transformClass(model, ClassTransform.transformingMethods(methods));
	}

	private static CodeTransform safePoints(CodeModel code) {
		Set<Label> safePoints = backwardTargets(code);
		for (CodeElement element : code)
			if (element instanceof ExceptionCatch handler)
				safePoints.add(handler.handler());
		boolean[] entered = { false };
		return (CodeBuilder builder, CodeElement element) -> {
			if (!entered[0]) {
				entered[0] = true;
				builder.invokestatic(KILLABLE, "check", CHECK);
			}
			builder.with(element);
			if (element instanceof LabelTarget label && safePoints.contains(label.label()))
				builder.invokestatic(KILLABLE, "check", CHECK);
		};
	}

	private static Set<Label> backwardTargets(CodeModel code) {
		Set<Label> seen = new HashSet<>();
		Set<Label> backward = new HashSet<>();
		for (CodeElement element : code) {
			if (element instanceof LabelTarget label)
				seen.add(label.label());
			else if (element instanceof BranchInstruction branch && seen.contains(branch.target()))
				backward.add(branch.target());
		}
		return backward;
	}
}
