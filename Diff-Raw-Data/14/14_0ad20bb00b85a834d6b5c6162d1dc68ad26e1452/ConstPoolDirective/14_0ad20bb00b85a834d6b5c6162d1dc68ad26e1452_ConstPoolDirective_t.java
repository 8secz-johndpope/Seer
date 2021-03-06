 /**
  * 
  */
 package v9t9.tools.asm.directive;
 
 import java.util.List;
 
 import v9t9.engine.cpu.IInstruction;
 import v9t9.tools.asm.Assembler;
 import v9t9.tools.asm.ResolveException;
 import v9t9.tools.asm.Symbol;
 import v9t9.tools.asm.operand.hl.AssemblerOperand;
 import v9t9.tools.asm.transform.ConstPool;
 
 /**
  * Indicate where the const table should live (automatically added)
  * @author Ed
  *
  */
 public class ConstPoolDirective extends Directive {
 
 	private ConstPool constPool;
 
 	public ConstPoolDirective(List<AssemblerOperand> ops) {
 		
 	}
 	@Override
 	public String toString() {
 		return ("$CONSTTABLE");
 	}
 	
 	/* (non-Javadoc)
 	 * @see v9t9.tools.asm.BaseAssemblerInstruction#resolve(v9t9.tools.asm.Assembler, v9t9.engine.cpu.IInstruction, boolean)
 	 */
 	@Override
 	public IInstruction[] resolve(Assembler assembler, IInstruction previous,
 			boolean finalPass) throws ResolveException {
 		
 		this.constPool = assembler.getConstPool();
 		Symbol symbol = constPool.getTableAddr();
 		if (symbol == null) {
 			return NO_INSTRUCTIONS;
 		}
 		
 		// go even
 		assembler.setPc((assembler.getPc() + 1) & 0xfffe);
 		setPc(assembler.getPc());
 
 		symbol.setAddr(assembler.getPc());

		// skip table
		assembler.setPc((assembler.getPc() + constPool.getSize() + 1) & 0xfffe);

 		return new IInstruction[] { this };
 	}
 	
 	public byte[] getBytes() {
 		return constPool.getBytes();
 	}
 	
 }
