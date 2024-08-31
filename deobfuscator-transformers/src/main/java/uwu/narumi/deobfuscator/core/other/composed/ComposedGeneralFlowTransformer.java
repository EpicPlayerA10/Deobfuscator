package uwu.narumi.deobfuscator.core.other.composed;

import uwu.narumi.deobfuscator.api.transformer.ComposedTransformer;
import uwu.narumi.deobfuscator.core.other.impl.clean.InvalidMethodCleanTransformer;
import uwu.narumi.deobfuscator.core.other.impl.clean.PeepholeCleanTransformer;
import uwu.narumi.deobfuscator.core.other.impl.clean.peephole.DeadCodeCleanTransformer;
import uwu.narumi.deobfuscator.core.other.impl.pool.InlineLocalVariablesTransformer;
import uwu.narumi.deobfuscator.core.other.impl.pool.InlineStaticFieldTransformer;
import uwu.narumi.deobfuscator.core.other.impl.universal.UniversalNumberTransformer;
import uwu.narumi.deobfuscator.core.other.impl.universal.UniversalFlowTransformer;

public class ComposedGeneralFlowTransformer extends ComposedTransformer {

  public ComposedGeneralFlowTransformer() {
    super(
        // Preparation
        InvalidMethodCleanTransformer::new,
        () -> new InlineStaticFieldTransformer(true, true),
        InlineLocalVariablesTransformer::new,

        // Main transformer
        UniversalFlowTransformer::new,

        // Clean up garbage
        PeepholeCleanTransformer::new
    );
  }
}
