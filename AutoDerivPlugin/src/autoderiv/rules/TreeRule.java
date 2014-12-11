package autoderiv.rules;

import static autoderiv.Debug.*;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import autoderiv.Rule;
import autoderiv.Tools;

/**@brief this Rule is used when the conf file specify a folder which is
 * recursively derived.
 * Useful for build temp folder for example.
 * rule is like :
 * 		.obj
 * 		.dep
 */
public class TreeRule implements Rule{

	// the tree node which is designated by the rule.
	private IPath path;
	private boolean isDerived;
	private IResource	member;
	private IProject project;

	public TreeRule(IProject project, IPath specificRes, boolean setAsDerived){
		this.project = project;
		path = specificRes;
		isDerived = setAsDerived;
		member = project.findMember(path);

		info("TreeRule created for project "+project.getName()+", path "+IPath.SEPARATOR+path.toOSString());
	}

	public void applyOnProject(final IProgressMonitor progress) {
		// initialization
		if(!checkIni()) return;

		// effective action
		try {
			member.accept(new IResourceVisitor() {
				@Override
				public boolean visit(IResource res) throws CoreException {
					Tools.checkedSetDerived(res, isDerived, progress);
					return true;
				}
			});
		} catch (CoreException e) { e.printStackTrace(); }
	}

	boolean checkIni(){
		if(member == null)
			member = project.findMember(path);
		return member != null && member.exists();
	}

	@Override
	public void applyOnResource(IResource res, IProgressMonitor progress) {
		// initialization
		if(!checkIni()) return;

		// check if the resource is handled by the rule
		boolean fits = false;
		switch(member.getType()){
		case IResource.FILE :
			fits = member.equals(res);
			break;
		case IResource.PROJECT:
		case IResource.ROOT:
			fits = true;
			break;
		case IResource.FOLDER :
			IFolder f = (IFolder) member;
//			info("TreeRule.applyOnResource() a " + f.exists(res.getLocation()));
//			info("TreeRule.applyOnResource() b " + f.exists(res.getFullPath()));
			IPath rp = res.getProjectRelativePath();
			IPath fp = f.getProjectRelativePath();
//			info("TreeRule.applyOnResource() c " + fp.isPrefixOf(rp));
//			info("TreeRule.applyOnResource() d " + (f.findMember(res.getLocation()) != null));
			fits = fp.isPrefixOf(rp);
			break;
		default:
			// should not happen: resource is not supposed to be a project or /
			warn("TreeRule.applyOnResource() NOT HANDLED");
		}

		// the resources matches the rule, apply derived attribute
		if(fits)
			Tools.checkedSetDerived_nt(res, isDerived, progress);
	}
}