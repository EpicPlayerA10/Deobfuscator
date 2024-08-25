// ASM: a very small and fast Java bytecode manipulation framework
// Copyright (c) 2000-2011 INRIA, France Telecom
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions
// are met:
// 1. Redistributions of source code must retain the above copyright
//    notice, this list of conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright
//    notice, this list of conditions and the following disclaimer in the
//    documentation and/or other materials provided with the distribution.
// 3. Neither the name of the copyright holders nor the names of its
//    contributors may be used to endorse or promote products derived from
//    this software without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
// AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
// LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
// CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
// SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
// INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
// CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
// THE POSSIBILITY OF SUCH DAMAGE.
package org.objectweb.asm.tree.analysis;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * An extended {@link SourceInterpreter} that keeps track of a
 * {@link SourceValue} that created another {@link SourceValue}.
 * <p>
 * The only modified method is {@link #copyOperation(AbstractInsnNode, OriginalSourceValue)}.
 */
public class OriginalSourceInterpreter extends Interpreter<OriginalSourceValue> implements Opcodes {

  /**
   * Constructs a new {@link SourceInterpreter} for the latest ASM API version. <i>Subclasses must
   * not use this constructor</i>. Instead, they must use the {@link #OriginalSourceInterpreter(int)}
   * version.
   */
  public OriginalSourceInterpreter() {
    super(/* latest api = */ ASM9);
    if (getClass() != OriginalSourceInterpreter.class) {
      throw new IllegalStateException();
    }
  }

  /**
   * Constructs a new {@link SourceInterpreter}.
   *
   * @param api the ASM API version supported by this interpreter. Must be one of the {@code
   *     ASM}<i>x</i> values in {@link Opcodes}.
   */
  protected OriginalSourceInterpreter(final int api) {
    super(api);
  }

  @Override
  public OriginalSourceValue newValue(final Type type) {
    if (type == Type.VOID_TYPE) {
      return null;
    }
    return new OriginalSourceValue(type == null ? 1 : type.getSize());
  }

  @Override
  public OriginalSourceValue newOperation(final AbstractInsnNode insn) {
    int size;
    switch (insn.getOpcode()) {
      case LCONST_0:
      case LCONST_1:
      case DCONST_0:
      case DCONST_1:
        size = 2;
        break;
      case LDC:
        Object value = ((LdcInsnNode) insn).cst;
        size = value instanceof Long || value instanceof Double ? 2 : 1;
        break;
      case GETSTATIC:
        size = Type.getType(((FieldInsnNode) insn).desc).getSize();
        break;
      default:
        size = 1;
        break;
    }
    return new OriginalSourceValue(size, insn);
  }

  @Override
  public OriginalSourceValue copyOperation(final AbstractInsnNode insn, final OriginalSourceValue value) {
    // OriginalSourceInterpreter start - Track the original value
    return new OriginalSourceValue(value, insn);
    // OriginalSourceInterpreter end
  }

  @Override
  public OriginalSourceValue unaryOperation(final AbstractInsnNode insn, final OriginalSourceValue value) {
    int size;
    switch (insn.getOpcode()) {
      case LNEG:
      case DNEG:
      case I2L:
      case I2D:
      case L2D:
      case F2L:
      case F2D:
      case D2L:
        size = 2;
        break;
      case GETFIELD:
        size = Type.getType(((FieldInsnNode) insn).desc).getSize();
        break;
      default:
        size = 1;
        break;
    }
    return new OriginalSourceValue(size, insn);
  }

  @Override
  public OriginalSourceValue binaryOperation(
      final AbstractInsnNode insn, final OriginalSourceValue value1, final OriginalSourceValue value2) {
    int size;
    switch (insn.getOpcode()) {
      case LALOAD:
      case DALOAD:
      case LADD:
      case DADD:
      case LSUB:
      case DSUB:
      case LMUL:
      case DMUL:
      case LDIV:
      case DDIV:
      case LREM:
      case DREM:
      case LSHL:
      case LSHR:
      case LUSHR:
      case LAND:
      case LOR:
      case LXOR:
        size = 2;
        break;
      default:
        size = 1;
        break;
    }
    return new OriginalSourceValue(size, insn);
  }

  @Override
  public OriginalSourceValue ternaryOperation(
      final AbstractInsnNode insn,
      final OriginalSourceValue value1,
      final OriginalSourceValue value2,
      final OriginalSourceValue value3) {
    return new OriginalSourceValue(1, insn);
  }

  @Override
  public OriginalSourceValue naryOperation(
      final AbstractInsnNode insn, final List<? extends OriginalSourceValue> values) {
    int size;
    int opcode = insn.getOpcode();
    if (opcode == MULTIANEWARRAY) {
      size = 1;
    } else if (opcode == INVOKEDYNAMIC) {
      size = Type.getReturnType(((InvokeDynamicInsnNode) insn).desc).getSize();
    } else {
      size = Type.getReturnType(((MethodInsnNode) insn).desc).getSize();
    }
    return new OriginalSourceValue(size, insn);
  }

  @Override
  public void returnOperation(
      final AbstractInsnNode insn, final OriginalSourceValue value, final OriginalSourceValue expected) {
    // Nothing to do.
  }

  @Override
  public OriginalSourceValue merge(final OriginalSourceValue value1, final OriginalSourceValue value2) {
    if (value1.insns instanceof SmallSet && value2.insns instanceof SmallSet) {
      Set<AbstractInsnNode> setUnion =
          ((SmallSet<AbstractInsnNode>) value1.insns)
              .union((SmallSet<AbstractInsnNode>) value2.insns);
      // OriginalSourceInterpreter start
      if (setUnion == value1.insns && value1.size == value2.size && Objects.equals(value1.copiedFrom, value2.copiedFrom)) {
      // OriginalSourceInterpreter end
        return value1;
      } else {
        // OriginalSourceInterpreter start
        OriginalSourceValue copiedFrom = null;
        if (setUnion.size() == 1 && value1.copiedFrom != null && value2.copiedFrom != null) {
          copiedFrom = this.merge(value1.copiedFrom, value2.copiedFrom);
        }
        return new OriginalSourceValue(Math.min(value1.size, value2.size), setUnion, copiedFrom);
        // OriginalSourceInterpreter end
      }
    }
    // OriginalSourceInterpreter start
    if (value1.size != value2.size || !containsAll(value1.insns, value2.insns) || !Objects.equals(value1.copiedFrom, value2.copiedFrom)) {
    // OriginalSourceInterpreter end
      HashSet<AbstractInsnNode> setUnion = new HashSet<>();
      setUnion.addAll(value1.insns);
      setUnion.addAll(value2.insns);

      // OriginalSourceInterpreter start
      OriginalSourceValue copiedFrom = null;
      if (setUnion.size() == 1 && value1.copiedFrom != null && value2.copiedFrom != null) {
        copiedFrom = this.merge(value1.copiedFrom, value2.copiedFrom);
      }
      return new OriginalSourceValue(Math.min(value1.size, value2.size), setUnion, copiedFrom);
      // OriginalSourceInterpreter end
    }
    return value1;
  }

  private static <E> boolean containsAll(final Set<E> self, final Set<E> other) {
    if (self.size() < other.size()) {
      return false;
    }
    return self.containsAll(other);
  }
}
