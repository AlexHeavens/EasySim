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
import java.lang.classfile.Annotation;
import java.lang.classfile.MethodModel;
import java.lang.classfile.MethodTransform;
import java.lang.classfile.attribute.RuntimeInvisibleAnnotationsAttribute;
import java.lang.classfile.instruction.BranchInstruction;
import java.lang.classfile.instruction.ExceptionCatch;
import java.lang.classfile.instruction.InvokeDynamicInstruction;
import java.lang.classfile.instruction.InvokeInstruction;
import java.lang.classfile.instruction.LabelTarget;
import java.lang.classfile.instruction.MonitorInstruction;
import java.lang.reflect.AccessFlag;
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;
import java.util.HashSet;
import java.util.Set;

/**
 * Adds a call to {@link KillableThread#check()} at the start of every method that has a call, a loop or a handler, at every loop head (the target of a
 * backward branch) and at the start of every exception handler of compiled user code, so a killed node cannot run
 * on in a loop or a recursion that never blocks. The check at a loop head can sit inside a <code>try</code> whose
 * handler swallows the {@link NodeKilledError}; the check at the handler's start throws it again from outside that
 * <code>try</code>, so the error leaves one level per handler. It also marks every method that has a call, a loop or a handler <code>@continuable</code>, so the Tascalate
 * agent lets the scheduler suspend a node from anywhere in its code. It rejects <code>synchronized</code>, because a
 * node that suspends inside it loses the monitor and lets other nodes in (ADR 0002). Without the check a thread cannot be stopped from outside on JDK 20 and later.
 */
public final class NodeInstrumenter {

	private static final ClassDesc KILLABLE = ClassDesc.of(KillableThread.class.getName());
	private static final MethodTypeDesc CHECK = MethodTypeDesc.of(ConstantDescs.CD_void);

	/** Marks a method for the Tascalate agent, so the node can suspend inside it (ADR 0001). */
	private static final ClassDesc CONTINUABLE = ClassDesc.of("org.apache.commons.javaflow.api.continuable");

	private NodeInstrumenter() {
	}

	public static byte[] instrument(byte[] classBytes) {
		ClassFile cf = ClassFile.of(ClassFile.ClassHierarchyResolverOption.of(ClassHierarchyResolver
				.defaultResolver().orElse(ClassHierarchyResolver.ofClassLoading(NodeInstrumenter.class.getClassLoader()))));
		ClassModel model = cf.parse(classBytes);
		rejectSynchronized(model);
		MethodTransform methods = (builder, element) -> {
			if (element instanceof CodeModel code && isLeaf(code)) {
				builder.with(element);
			} else if (element instanceof CodeModel code) {
				builder.with(RuntimeInvisibleAnnotationsAttribute.of(Annotation.of(CONTINUABLE)));
				builder.transformCode(code, safePoints(code));
			} else if (!(element instanceof RuntimeInvisibleAnnotationsAttribute)) {
				builder.with(element);
			}
		};
		return cf.transformClass(model, ClassTransform.transformingMethods(methods));
	}

	/**
	 * A suspended continuation does not keep its monitors: two nodes can then be inside the same
	 * <code>synchronized</code> block at once. Nodes already run one at a time per worker and talk through messages,
	 * so the student gets a clear error and does not get a lock that silently fails.
	 */
	private static void rejectSynchronized(ClassModel model) {
		for (MethodModel method : model.methods()) {
			boolean synced = method.flags().has(AccessFlag.SYNCHRONIZED);
			if (!synced && method.code().isPresent())
				synced = method.code().get().elementList().stream().anyMatch(MonitorInstruction.class::isInstance);
			if (synced)
				throw new UnsupportedNodeCodeException("Node code cannot use 'synchronized' (found in "
						+ model.thisClass().asInternalName().replace('/', '.') + "." + method.methodName().stringValue()
						+ "). A node can pause inside it, and then another node enters the same lock. "
						+ "Nodes share nothing: send a message instead.");
		}
	}

	/**
	 * A method with no call, no loop and no exception handler runs straight through, so it can neither suspend nor
	 * run long. Leaving it alone saves the check and the continuation bookkeeping on every call of a getter or a
	 * small helper (ADR 0002 measures the saving).
	 */
	private static boolean isLeaf(CodeModel code) {
		if (!backwardTargets(code).isEmpty())
			return false;
		for (CodeElement element : code)
			if (element instanceof InvokeInstruction || element instanceof InvokeDynamicInstruction
					|| element instanceof ExceptionCatch)
				return false;
		return true;
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
