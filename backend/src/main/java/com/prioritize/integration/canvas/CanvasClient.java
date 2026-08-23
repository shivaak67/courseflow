package com.prioritize.integration.canvas;

import java.util.List;

public interface CanvasClient {

    List<CanvasCourseData> listCourses();

    List<CanvasAssignmentData> listAssignments(String canvasCourseId);
}
