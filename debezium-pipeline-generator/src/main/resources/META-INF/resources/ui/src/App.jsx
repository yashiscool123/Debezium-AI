import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { Box, CssBaseline } from '@mui/material';
import Header from './components/Header';
import Dashboard from './pages/Dashboard';
import PipelineBuilder from './pages/PipelineBuilder';
import MappingDesigner from './pages/MappingDesigner';
import PipelinePreview from './pages/PipelinePreview';
import DeploymentManager from './pages/DeploymentManager';
import Settings from './pages/Settings';

function App() {
  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', minHeight: '100vh' }}>
      <Header />
      <Box component="main" sx={{ flexGrow: 1, p: 3 }}>
        <Routes>
          <Route path="/" element={<Navigate to="/dashboard" replace />} />
          <Route path="/dashboard" element={<Dashboard />} />
          <Route path="/pipelines/new" element={<PipelineBuilder />} />
          <Route path="/pipelines/:id/edit" element={<PipelineBuilder />} />
          <Route path="/pipelines/:id/mapping" element={<MappingDesigner />} />
          <Route path="/pipelines/:id/preview" element={<PipelinePreview />} />
          <Route path="/pipelines/:id/deploy" element={<DeploymentManager />} />
          <Route path="/settings" element={<Settings />} />
          <Route path="*" element={<Navigate to="/dashboard" replace />} />
        </Routes>
      </Box>
    </Box>
  );
}

export default App;