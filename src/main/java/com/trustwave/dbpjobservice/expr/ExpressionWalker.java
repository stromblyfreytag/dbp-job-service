package com.trustwave.dbpjobservice.expr;

/**
 * <p>Interface for walking over expression tree.</p>
 * <p>Expression tree is traversed in the evaluation order, 
 * 'visit' method is called in every expression node.</p>
 * <p>First parameter of the visit() contains expression node itself, arg[] array contains results
 *   of visiting operation argument nodes.</p>
 *   <br/>
 *   <table>
 *   <tr align="left">
 *   <th>Operation</th>       <th># args</th><th>Arguments</th></tr>
 *   <tr><td>Property</td>    <td>1</td><td>property name</td></tr>
 *   <tr><td>Constant</td>    <td>1</td><td>constant value (String or Long, or Boolean)</td></tr>
 *   <tr><td>+,-,*,/,%</td>   <td>2</td><td>results of visiting left and right operands</td></tr>
 *   <tr><td>=,!=,~,!~</td>   <td>2</td><td>results of visiting left and right operands</td></tr>
 *   <tr><td>&gt;,&lt;,&gt;=,&lt;=</td>
 *                            <td>2</td><td>results of visiting left and right operands</td></tr>
 *   <tr><td>IN</td>          <td>2</td><td>result of visiting expression and in-list</td></tr>
 *   <tr><td>AND,OR</td>      <td>2</td><td>results of visiting left and right operands</td></tr>
 *   <tr><td>NOT</td>         <td>1</td><td>result of visiting operand</td></tr>
 *   <tr><td>-</td>           <td>1</td><td>(unary minus) result of visiting operand</td></tr>
 *   <tr><td>exists,empty</td><td>1</td><td>result of visiting operand</td></tr>
 *   <tr><td>isNumber</td>    <td>1</td><td>result of visiting operand</td></tr>
 *   <tr><td>Conditional</td> <td>3</td><td>(e.g. true? a: b), results of visiting condition, 'ifyes', 'otherwise'</td></tr>
 *   <tr><td>?:</td>          <td>2</td><td>(property with default),results of visiting property and default expression</td></tr>
 *   <tr><td>methodcall()</td><td>3</td><td>result of visiting expression, method name, arg list</td></tr>
 *   </table>  
 *   <br/>
 * 
 * @author vlad
 *
 */
public interface ExpressionWalker 
{
	/**
	 * <p>Called on visit expression node in the expression tree.</p>
	 * @param expression visited node
	 * @param arg results of visiting arguments, see table above
	 * @return arbitrary object, will be used as argument in parent nodes visit().
	 */
	public Object visit( Expression expression, Object ... arg );
}
