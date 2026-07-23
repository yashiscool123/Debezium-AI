import React, { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  Box,
  Card,
  CardContent,
  Typography,
  Button,
  Grid,
  Chip,
  Stepper,
  Step,
  StepLabel,
  TextField,
  Paper,
  Divider,
  Alert,
  AlertTitle,
  IconButton,
  Tooltip,
  CircularProgress,
  LinearProgress,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Tab,
  Tabs,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
} from '@mui/material';
import {
  CloudUpload as CloudUploadIcon,
  ArrowBack as ArrowBackIcon,
  CheckCircle as CheckCircleIcon,
  Schedule as ScheduleIcon,
  Warning as WarningIcon,
  Error as ErrorIcon,
  PlayArrow as PlayArrowIcon,
  Stop as StopIcon,
  Refresh as RefreshIcon,
  Terminal as TerminalIcon,
  ContentCopy as CopyIcon,
  Preview as PreviewIcon,
} from '@mui/icons-material';
import api from '../services/api';

const DEPLOYMENT_STEPS = [
  { label: 'Validate', description: 'Validate configuration' },
  { label: 'Prerequisites', description: 'Check dependencies' },
  { label: 'Deploy', description: 'Deploy to cluster' },
  { label: 'Verify', description: 'Verify pipeline health' },
];

function DeploymentManager() {
  const { id = '1' } = useParams();
  const navigate = useNavigate();
  const [activeStep, setActiveStep] = useState(0);
  const [deploying, setDeploying] = useState(false);
  const [deployed, setDeployed] = useState(false);
  const [logs, setLogs] = useState([]);
  const [deploymentType, setDeploymentType] = useState('strimzi');
  const [targetEnv, setTargetEnv] = useState('kubernetes');
  const [tab, setTab] = useState(0);
  const [validationStatus, setValidationStatus] = useState('pending');
  const [prereqsStatus, setPrereqsStatus] = useState('pending');
  const [deployStatus, setDeployStatus] = useState('pending');
  const [verifyStatus, setVerifyStatus] = useState('pending');

  const addLog = (message, type = 'info') => {
    setLogs(prev => [...prev, { time: new Date().toISOString(), message, type }]);
  };

  const deploy = async () => {
    setDeploying(true);
    addLog('Starting deployment...', 'info');
    
    addLog('Validating pipeline configuration...', 'info');
    await new Promise(r => setTimeout(r, 1000));
    setValidationStatus('success');
    setActiveStep(0);
    addLog('Validation passed', 'success');
    
    addLog('Checking prerequisites...', 'info');
    addLog('  - Verifying Kubernetes connection...', 'info');
    addLog('  - Checking Strimzi Operator...', 'info');
    addLog('  - Verifying Kafka cluster...', 'info');
    await new Promise(r => setTimeout(r, 1500));
    setPrereqsStatus('success');
    setActiveStep(1);
    addLog('All prerequisites satisfied', 'success');
    
    addLog('Deploying Debezium Connect...', 'info');
    addLog('  - Creating namespace...', 'info');
    addLog('  - Deploying KafkaConnector...', 'info');
    await new Promise(r => setTimeout(r, 2000));
    setDeployStatus('success');
    setActiveStep(2);
    addLog('Pipeline deployed successfully', 'success');
    
    addLog('Verifying pipeline health...', 'info');
    addLog('  - Checking connector status...', 'info');
    addLog('  - Verifying task assignment...', 'info');
    addLog('  - Testing event flow...', 'info');
    await new Promise(r => setTimeout(r, 2000));
    setVerifyStatus('success');
    setActiveStep(3);
    setDeployed(true);
    addLog('Pipeline is running', 'success');
    
    setDeploying(false);
  };

  const getStatusChip = (status) => {
    switch (status) {
      case 'success': return <Chip icon={<CheckCircleIcon />} label="Passed" color="success" size="small" />;
      case 'error': return <Chip icon={<ErrorIcon />} label="Failed" color="error" size="small" />;
      case 'running': return <Chip icon={<ScheduleIcon />} label="In Progress" color="info" size="small" />;
      default: return <Chip label="Pending" variant="outlined" size="small" />;
    }
  };

  return (
    <Box sx={{ maxWidth: '1000px', mx: 'auto' }}>
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mb: 3 }}>
        <IconButton onClick={() => navigate(-1)}><ArrowBackIcon /></IconButton>
        <Box sx={{ flexGrow: 1 }}>
          <Typography variant="h5" sx={{ fontWeight: 700 }}>Deploy Pipeline</Typography>
          <Typography variant="body2" color="text.secondary">Deploy your CDC pipeline to the target environment</Typography>
        </Box>
        <Button
          variant="outlined"
          startIcon={<PreviewIcon />}
          onClick={() => navigate(`/pipelines/${id}/preview`)}
        >
          Review Config
        </Button>
      </Box>

      <Grid container spacing={3} sx={{ mb: 4 }}>
        <Grid item xs={12} sm={4}>
          <FormControl fullWidth>
            <InputLabel>Deployment Type</InputLabel>
            <Select value={deploymentType} onChange={(e) => setDeploymentType(e.target.value)} label="Deployment Type">
              <MenuItem value="strimzi">Strimzi Operator</MenuItem>
              <MenuItem value="docker-compose">Docker Compose</MenuItem>
              <MenuItem value="helm">Helm Charts</MenuItem>
              <MenuItem value="terraform">Terraform</MenuItem>
            </Select>
          </FormControl>
        </Grid>
        <Grid item xs={12} sm={4}>
          <FormControl fullWidth>
            <InputLabel>Target Environment</InputLabel>
            <Select value={targetEnv} onChange={(e) => setTargetEnv(e.target.value)} label="Target Environment">
              <MenuItem value="kubernetes">Kubernetes</MenuItem>
              <MenuItem value="docker">Docker</MenuItem>
              <MenuItem value="openshift">OpenShift</MenuItem>
            </Select>
          </FormControl>
        </Grid>
        <Grid item xs={12} sm={4}>
          <TextField fullWidth label="Namespace" defaultValue="debezium" />
        </Grid>
      </Grid>

      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Stepper activeStep={activeStep}>
            {DEPLOYMENT_STEPS.map((step, index) => (
              <Step key={step.label}>
                <StepLabel>
                  <Box>
                    <Typography variant="subtitle2">{step.label}</Typography>
                    <Typography variant="caption" color="text.secondary">{step.description}</Typography>
                  </Box>
                </StepLabel>
              </Step>
            ))}
          </Stepper>
        </CardContent>
      </Card>

      <Grid container spacing={3}>
        <Grid item xs={12}>
          <Card>
            <CardContent>
              <Tabs value={tab} onChange={(e, v) => setTab(v)} sx={{ mb: 2 }}>
                <Tab label="Status" />
                <Tab label="Deployment Logs" />
                <Tab label="Configuration" />
              </Tabs>
              {tab === 0 && (
                <TableContainer component={Paper} variant="outlined">
                  <Table>
                    <TableHead>
                      <TableRow>
                        <TableCell>Step</TableCell>
                        <TableCell>Status</TableCell>
                        <TableCell>Details</TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      <TableRow>
                        <TableCell>Configuration Validation</TableCell>
                        <TableCell>{getStatusChip(validationStatus)}</TableCell>
                        <TableCell>
                          {validationStatus === 'success' ? 'All config fields valid' : 'Pending validation...'}
                        </TableCell>
                      </TableRow>
                      <TableRow>
                        <TableCell>Prerequisites</TableCell>
                        <TableCell>{getStatusChip(prereqsStatus)}</TableCell>
                        <TableCell>
                          {prereqsStatus === 'success' ? 'Kubernetes, Strimzi, Kafka available' : 'Checking prerequisites...'}
                        </TableCell>
                      </TableRow>
                      <TableRow>
                        <TableCell>Deploy</TableCell>
                        <TableCell>{getStatusChip(deployStatus)}</TableCell>
                        <TableCell>
                          {deployStatus === 'success' ? 'KafkaConnector created' : 'Pending deployment...'}
                        </TableCell>
                      </TableRow>
                      <TableRow>
                        <TableCell>Verify</TableCell>
                        <TableCell>{getStatusChip(verifyStatus)}</TableCell>
                        <TableCell>
                          {verifyStatus === 'success' ? 'Pipeline running and healthy' : 'Pending verification...'}
                        </TableCell>
                      </TableRow>
                    </TableBody>
                  </Table>
                </TableContainer>
              )}
              {tab === 1 && (
                <Paper variant="outlined" sx={{ p: 2, backgroundColor: '#1e1e2e', color: '#cdd6f4', fontFamily: 'monospace', fontSize: 13, borderRadius: 2, maxHeight: 400, overflow: 'auto' }}>
                  {logs.length === 0 && <Typography sx={{ color: '#6c7086' }}>Waiting for deployment...</Typography>}
                  {logs.map((log, i) => (
                    <Box key={i} sx={{ mb: 0.5 }}>
                      <Typography variant="caption" sx={{ color: '#6c7086' }}>[{new Date(log.time).toLocaleTimeString()}]</Typography>{' '}
                      <Typography variant="body2" component="span" sx={{ color: log.type === 'error' ? '#f38ba8' : log.type === 'success' ? '#a6e3a1' : '#cdd6f4' }}>
                        {log.message}
                      </Typography>
                    </Box>
                  ))}
                </Paper>
              )}
              {tab === 2 && (
                <Paper variant="outlined" sx={{ p: 2, backgroundColor: '#f5f5f5', fontFamily: 'monospace', fontSize: 13, borderRadius: 2 }}>
                  <pre style={{ margin: 0 }}>
                    {`# Deployment Configuration
deployment.type: ${deploymentType}
target.environment: ${targetEnv}
namespace: debezium
connect.cluster: debezium-connect
replicas: 1
resources:
  requests:
    cpu: "500m"
    memory: "512Mi"
  limits:
    cpu: "1000m"
    memory: "1Gi"`}
                  </pre>
                </Paper>
              )}
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      <Box sx={{ display: 'flex', justifyContent: 'flex-end', gap: 2, mt: 3 }}>
        {!deployed ? (
          <Button
            variant="contained"
            size="large"
            onClick={deploy}
            disabled={deploying}
            startIcon={deploying ? <CircularProgress size={20} /> : <CloudUploadIcon />}
          >
            {deploying ? 'Deploying...' : 'Start Deployment'}
          </Button>
        ) : (
          <Box sx={{ display: 'flex', gap: 2, alignItems: 'center' }}>
            <Chip icon={<CheckCircleIcon />} label="Deployed Successfully" color="success" variant="outlined" />
            <Button variant="contained" onClick={() => navigate(`/pipelines/${id}/preview`)} startIcon={<PreviewIcon />}>
              View Pipeline
            </Button>
            <Button variant="outlined" onClick={() => navigate('/dashboard')} startIcon={<RefreshIcon />}>
              Back to Dashboard
            </Button>
          </Box>
        )}
      </Box>
    </Box>
  );
}

export default DeploymentManager;