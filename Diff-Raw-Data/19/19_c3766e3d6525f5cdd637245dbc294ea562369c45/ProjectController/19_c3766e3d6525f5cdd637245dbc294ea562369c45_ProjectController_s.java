 package controllers;
 
 import play.*;
 import play.mvc.*;
 import play.data.*;
 
 import views.html.*;
 import models.*;
 
 public class ProjectController extends Controller {
 
 	static Form<Project> projectForm = form(Project.class);
 
<<<<<<< HEAD
	 public static Result projects() {
     	return ok(project.render(Project.findAllProject(),projectForm));
  	}
=======
 	public static Result projects() {
 		return ok(project.render(Project.findAllProject(),projectForm));
 	}
 
>>>>>>> reindent and fix bug
   	public static Result deleteProject(Long id) {
 		Project.delete(id);
 		return redirect(routes.ProjectController.projects());
   	}
<<<<<<< HEAD
  	public static Result projectsList() {
      return ok(projectlist.render(Project.findAllProject()));
    }
=======
 
>>>>>>> reindent and fix bug
 	public static Result addProject() {
 		Form<Project> filledForm = projectForm.bindFromRequest();
 
 		if(filledForm.hasErrors()) {
 			return badRequest(views.html.project.render(Project.findAllProject(),projectForm));
 		} 
 
 		else{
 			if (Project.checkExistProject(filledForm.get())) {
 				Project.createProject(filledForm.get());
 				return redirect(routes.ProjectController.projects());
 			}
 			else {
 				return redirect(routes.ProjectController.projects());
 			}
 		}
 	}
 }
