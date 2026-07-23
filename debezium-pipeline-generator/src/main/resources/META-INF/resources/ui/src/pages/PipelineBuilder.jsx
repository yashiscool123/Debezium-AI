import React, { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import {
  Stepper,
  Step,
  StepLabel,
  Box,
  Button,
  Grid,
  Card,
  CardContent,
  Typography,
  TextField,
  Select,
  MenuItem,
  FormControl,
  InputLabel,
  Switch,
  Divider,
  Alert,
  AlertTitle,
  Tabs,
  Tab,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  IconButton,
  Tooltip,
  Chip,
  Accordion,
  AccordionSummary,
  AccordionDetails,
  ExpandMoreIcon,
  Autocomplete,
  Slide,
} from '@mui/material';
import {
  Database as DatabaseIcon,
  Schema as SchemaIcon,
  Settings as SettingsIcon,
  CloudUpload as CloudUploadIcon,
  Save as SaveIcon,
  PlayArrow as PlayArrowIcon,
  CheckCircle as CheckCircleIcon,
  Edit as EditIcon,
  Add as AddIcon,
  Delete as DeleteIcon,
  ContentCopy as CopyIcon,
} from '@mui/icons-material';
import api from '../services/api';

const CONNECTOR_TYPES = [
  { id: 'mysql', name: 'MySQL', icon: <DatabaseIcon /> },
  { id: 'postgresql', name: 'PostgreSQL', icon: <DatabaseIcon /> },
  { id: 'mongodb', name: 'MongoDB', icon: <DatabaseIcon /> },
  { id: 'sqlserver', name: 'SQL Server', icon: <DatabaseIcon /> },
  { id: 'oracle', name: 'Oracle', icon: <DatabaseIcon /> },
  { id: 'mariadb', name: 'MariaDB', icon: <DatabaseIcon /> },
  { id: 'jdbc', name: 'JDBC (Generic)', icon: <DatabaseIcon /> },
];

const DEPLOYMENT_TYPES = [
  { id: 'strimzi', name: 'Kubernetes (Strimzi)', description: 'Deploy to Kubernetes using Strimzi Operator' },
  { id: 'docker-compose', name: 'Docker Compose', description: 'Local development with Docker Compose' },
  { id: 'helm', name: 'Helm Charts', description: 'Deploy using Helm charts' },
];

function PipelineBuilder() {
  const navigate = useNavigate();
  const { id } = useParams();
  const isEditing = Boolean(id);

  const [activeStep, setActiveStep] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);

  const [formData, setFormData] = useState({
    name: '',
    description: '',
    source: {
      type: 'mysql',
      host: '',
      port: 3306,
      username: '',
      password: '',
      databaseName: '',
      tables: [],
      config: {},
    },
    target: {
      type: 'kafka',
      host: '',
      port: 9092,
      username: '',
      password: '',
      databaseName: '',
      config: {},
    },
    kafka: {
      bootstrapServers: 'localhost:9092',
      topicPrefix: '',
      topicNamingStrategy: 'default',
      partitions: 1,
      replicationFactor: 1,
    },
    schemaRegistry: {
      type: 'apicurio',
      url: 'http://localhost:8081',
      schemaCompatibility: 'BACKWARD',
    },
    topicNamingStrategy: 'default',
    topicNamingPlaceholders: {},
    transformation: {
      type: 'smt_chain',
      steps: [],
    },
    deployment: {
      type: 'strimzi',
      namespace: 'debezium',
      connectClusterName: 'debezium-connect',
      replicas: 1,
    },
    mappings: [],
  });

  const steps = [
    { label: 'Basic Info', description: 'Pipeline name and description' },
    { label: 'Source', description: 'Configure source database' },
    { label: 'Target', description: 'Configure target system' },
    { label: 'Kafka & Schema', description: 'Kafka and Schema Registry settings' },
    { label: 'Transformations', description: 'Define data transformations' },
    { label: 'Deployment', description: 'Choose deployment target' },
    { label: 'Review', description: 'Review and generate pipeline' },
  ];

  const handleNext = () => setActiveStep((prev) => Math.min(prev + 1, steps.length - 1));
  const handleBack = () => setActiveStep((prev) => Math.max(prev - 1, 0));

  const updateField = (path, value) => {
    setFormData((prev) => {
      const newData = JSON.parse(JSON.stringify(prev));
      const keys = path.split('.');
      let obj = newData;
      for (let i = 0; i < keys.length - 1; i++) {
        obj = obj[keys[i]];
      }
      obj[keys[keys.length - 1]] = value;
      return newData;
    });
  };

  const validateStep = () => {
    switch (activeStep) {
      case 0:
        return formData.name && formData.name.trim().length > 0;
      case 1:
        return formData.source.host && formData.source.username && formData.source.databaseName;
      case 2:
        return true; // Target is optional for Kafka-only
      case 3:
        return formData.kafka.bootstrapServers && formData.schemaRegistry.url;
      default:
        return true;
    }
  };

  const handleSubmit = async (deploy = false) => {
    setLoading(true);
    setError(null);
    try {
      const response = await api.post('/pipelines/generate', formData);
      const pipeline = response.data.spec;
      setSuccess('Pipeline generated successfully!');
      if (deploy) {
        navigate(`/pipelines/${pipeline.id}/deploy`);
      } else {
        navigate(`/pipelines/${pipeline.id}/preview`);
      }
    } catch (err) {
      setError(err.response?.data?.error || 'Failed to generate pipeline');
    } finally {
      setLoading(false);
    }
  };

  const renderStepContent = () => {
    switch (activeStep) {
      case 0:
        return (
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>Pipeline Information</Typography>
              <Grid container spacing={3}>
                <Grid item xs={12} sm={6}>
                  <TextField
                    fullWidth
                    label="Pipeline Name"
                    value={formData.name}
                    onChange={(e) => updateField('name', e.target.value)}
                    required
                    placeholder="e.g., MySQL Orders to Kafka"
                  />
                </Grid>
                <Grid item xs={12} sm={6}>
                  <TextField
                    fullWidth
                    label="Description"
                    value={formData.description}
                    onChange={(e) => updateField('description', e.target.value)}
                    multiline
                    rows={4}
                    placeholder="Describe what this pipeline does..."
                  />
                </Grid>
              </Grid>
            </CardContent>
          </Card>
        );
      case 1:
        return (
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>Source Database Configuration</Typography>
              <Grid container spacing={3}>
                <Grid item xs={12} sm={6}>
                  <FormControl fullWidth>
                    <InputLabel>Database Type</InputLabel>
                    <Select
                      value={formData.source.type}
                      onChange={(e) => updateField('source.type', e.target.value)}
                      label="Database Type"
                    >
                      {CONNECTOR_TYPES.map((t) => (
                        <MenuItem key={t.id} value={t.id}>
                          {t.icon} {t.name}
                        </MenuItem>
                      ))}
                    </Select>
                  </FormControl>
                </Grid>
                <Grid item xs={12} sm={6}>
                  <TextField
                    fullWidth
                    label="Host"
                    value={formData.source.host}
                    onChange={(e) => updateField('source.host', e.target.value)}
                    placeholder="localhost"
                  />
                </Grid>
                <Grid item xs={12} sm={3}>
                  <TextField
                    fullWidth
                    label="Port"
                    type="number"
                    value={formData.source.port}
                    onChange={(e) => updateField('source.port', parseInt(e.target.value) || 0)}
                    defaultValue={3306}
                  />
                </Grid>
                <Grid item xs={12} sm={3}>
                  <TextField
                    fullWidth
                    label="Database Name"
                    value={formData.source.databaseName}
                    onChange={(e) => updateField('source.databaseName', e.target.value)}
                    placeholder="mydb"
                  />
                </Grid>
                <Grid item xs={12} sm={6}>
                  <TextField
                    fullWidth
                    label="Username"
                    value={formData.source.username}
                    onChange={(e) => updateField('source.username', e.target.value)}
                    placeholder="dbuser"
                  />
                </Grid>
                <Grid item xs={12} sm={6}>
                  <TextField
                    fullWidth
                    label="Password"
                    type="password"
                    value={formData.source.password}
                    onChange={(e) => updateField('source.password', e.target.value)}
                    placeholder="********"
                    helperText="Leave empty to use environment variable"
                  />
                </Grid>
                <Grid item xs={12}>
                  <Divider />
                  <Typography variant="subtitle1" gutterBottom>Advanced Configuration</Typography>
                  <TextField
                    fullWidth
                    label="Additional Config (JSON)"
                    value={JSON.stringify(formData.source.config, null, 2)}
                    onChange={(e) => {
                      try {
                        updateField('source.config', JSON.parse(e.target.value));
                      } catch {}
                    }}
                    multiline
                    rows={6}
                    placeholder='{"snapshot.mode": "initial", "table.include.list": "public.*"}'
                  />
                </Grid>
              </Grid>
            </CardContent>
          </Card>
        );
      case 2:
        return (
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>Target Configuration</Typography>
              <Box sx={{ mb: 3 }}>
                <Switch
                  checked={!!formData.target.type && formData.target.type !== 'kafka'}
                  onChange={(e) => {
                    if (!e.target.checked) {
                      updateField('target.type', 'kafka');
                    }
                  }}
                >
                  Configure Target Database (optional - leave off for Kafka-only)
                </Switch>
              </Box>
              {formData.target.type && formData.target.type !== 'kafka' && (
                <Grid container spacing={3}>
                  <Grid item xs={12} sm={6}>
                    <FormControl fullWidth>
                      <InputLabel>Target Type</InputLabel>
                      <Select
                        value={formData.target.type}
                        onChange={(e) => updateField('target.type', e.target.value)}
                        label="Target Type"
                      >
                        {CONNECTOR_TYPES.map((t) => (
                          <MenuItem key={t.id} value={t.id}>
                            {t.icon} {t.name}
                          </MenuItem>
                        ))}
                      </Select>
                    </FormControl>
                  </Grid>
                  <Grid item xs={12} sm={6}>
                    <TextField
                      fullWidth
                      label="Host"
                      value={formData.target.host}
                      onChange={(e) => updateField('target.host', e.target.value)}
                    />
                  </Grid>
                  <Grid item xs={12} sm={3}>
                    <TextField
                      fullWidth
                      label="Port"
                      type="number"
                      value={formData.target.port}
                      onChange={(e) => updateField('target.port', parseInt(e.target.value) || 0)}
                    />
                  </Grid>
                  <Grid item xs={12} sm={3}>
                    <TextField
                      fullWidth
                      label="Database/Schema"
                      value={formData.target.databaseName}
                      onChange={(e) => updateField('target.databaseName', e.target.value)}
                    />
                  </Grid>
                  <Grid item xs={12} sm={6}>
                    <TextField
                      fullWidth
                      label="Username"
                      value={formData.target.username}
                      onChange={(e) => updateField('target.username', e.target.value)}
                    />
                  </Grid>
                  <Grid item xs={12} sm={6}>
                    <TextField
                      fullWidth
                      label="Password"
                      type="password"
                      value={formData.target.password}
                      onChange={(e) => updateField('target.password', e.target.value)}
                    />
                  </Grid>
                </Grid>
              )}
            </CardContent>
          </Card>
        );
      case 3:
        return (
          <Card>
            <CardContent>
              <Tabs value={0} variant="fullWidth" sx={{ mb: 3 }}>
                <Tab label="Kafka" />
                <Tab label="Schema Registry" />
                <Tab label="Topic Naming" />
              </Tabs>
              <Divider sx={{ mb: 3 }} />
              <Box>
                <Typography variant="h6" gutterBottom>Kafka Configuration</Typography>
                <Grid container spacing={3}>
                  <Grid item xs={12} sm={6}>
                    <TextField
                      fullWidth
                      label="Bootstrap Servers"
                      value={formData.kafka.bootstrapServers}
                      onChange={(e) => updateField('kafka.bootstrapServers', e.target.value)}
                      placeholder="localhost:9092"
                    />
                  </Grid>
                  <Grid item xs={12} sm={3}>
                    <TextField
                      fullWidth
                      label="Topic Prefix"
                      value={formData.kafka.topicPrefix}
                      onChange={(e) => updateField('kafka.topicPrefix', e.target.value)}
                      placeholder="dbserver1"
                    />
                  </Grid>
                  <Grid item xs={12} sm={3}>
                    <FormControl fullWidth>
                      <InputLabel>Topic Naming Strategy</InputLabel>
                      <Select
                        value={formData.kafka.topicNamingStrategy}
                        onChange={(e) => updateField('kafka.topicNamingStrategy', e.target.value)}
                        label="Topic Naming Strategy"
                      >
                        <MenuItem value="default">Default (database.schema.table)</MenuItem>
                        <MenuItem value="topic_prefix">Prefix + table</MenuItem>
                        <MenuItem value="schema_table">Schema + table</MenuItem>
                      </Select>
                    </FormControl>
                  </Grid>
                </Grid>
                <Divider sx={{ my: 3 }} />
                <Typography variant="h6" gutterBottom>Schema Registry</Typography>
                <Grid container spacing={3}>
                  <Grid item xs={12} sm={6}>
                    <FormControl fullWidth>
                      <InputLabel>Registry Type</InputLabel>
                      <Select
                        value={formData.schemaRegistry.type}
                        onChange={(e) => updateField('schemaRegistry.type', e.target.value)}
                        label="Registry Type"
                      >
                        <MenuItem value="apicurio">Apicurio Registry</MenuItem>
                        <MenuItem value="confluent">Confluent Schema Registry</MenuItem>
                      </Select>
                    </FormControl>
                  </Grid>
                  <Grid item xs={12} sm={6}>
                    <TextField
                      fullWidth
                      label="Registry URL"
                      value={formData.schemaRegistry.url}
                      onChange={(e) => updateField('schemaRegistry.url', e.target.value)}
                      placeholder="http://localhost:8081"
                    />
                  </Grid>
                  <Grid item xs={12} sm={6}>
                    <FormControl fullWidth>
                      <InputLabel>Compatibility</InputLabel>
                      <Select
                        value={formData.schemaRegistry.schemaCompatibility}
                        onChange={(e) => updateField('schemaRegistry.schemaCompatibility', e.target.value)}
                        label="Compatibility Level"
                      >
                        <MenuItem value="BACKWARD">BACKWARD</MenuItem>
                        <MenuItem value="FORWARD">FORWARD</MenuItem>
                        <MenuItem value="FULL">FULL</MenuItem>
                        <MenuItem value="NONE">NONE</MenuItem>
                      </Select>
                    </FormControl>
                  </Grid>
                </Grid>
              </Box>
            </CardContent>
          </Card>
        );
      case 4:
        return (
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>Transformations (SMT Chain)</Typography>
              <TextField
                fullWidth
                label="Transformations (JSON)"
                value={JSON.stringify(formData.transformation, null, 2)}
                onChange={(e) => {
                  try {
                    updateField('transformation', JSON.parse(e.target.value));
                  } catch {}
                }}
                multiline
                rows={10}
                placeholder='{
  "type": "smt_chain",
  "steps": [
    {"name": "unwrap", "type": "io.debezium.transforms.ExtractNewRecordState", "config": {"drop.tombstones": "false"}},
    {"name": "flatten", "type": "org.apache.kafka.connect.transforms.Flatten$Value", "config": {}}
  ]
}'
              />
              <Typography variant="caption" color="text.secondary" sx={{ mt: 1, display: 'block' }}>
                Common SMTs: ExtractNewRecordState, Flatten, MaskField, ReplaceField, Cast, RegexRouter, TimestampRouter
              </Typography>
            </CardContent>
          </Card>
        );
      case 5:
        return (
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>Deployment Target</Typography>
              <Grid container spacing={3}>
                <Grid item xs={12}>
                  <FormControl fullWidth>
                    <InputLabel>Deployment Type</InputLabel>
                    <Select
                      value={formData.deployment.type}
                      onChange={(e) => updateField('deployment.type', e.target.value)}
                      label="Deployment Type"
                    >
                      {DEPLOYMENT_TYPES.map((t) => (
                        <MenuItem key={t.id} value={t.id}>
                          <Box sx={{ display: 'flex', flexDirection: 'column' }}>
                            <Typography variant="body2">{t.name}</Typography>
                            <Typography variant="caption" color="text.secondary">{t.description}</Typography>
                          </Box>
                        </MenuItem>
                      ))}
                    </Select>
                  </FormControl>
                </Grid>
                <Grid item xs={12} sm={6}>
                  <TextField
                    fullWidth
                    label="Kubernetes Namespace"
                    value={formData.deployment.namespace}
                    onChange={(e) => updateField('deployment.namespace', e.target.value)}
                    placeholder="debezium"
                  />
                </Grid>
                <Grid item xs={12} sm={6}>
                  <TextField
                    fullWidth
                    label="Connect Cluster Name"
                    value={formData.deployment.connectClusterName}
                    onChange={(e) => updateField('deployment.connectClusterName', e.target.value)}
                    placeholder="debezium-connect"
                  />
                </Grid>
                <Grid item xs={12} sm={6}>
                  <TextField
                    fullWidth
                    label="Replicas"
                    type="number"
                    value={formData.deployment.replicas}
                    onChange={(e) => updateField('deployment.replicas', parseInt(e.target.value) || 1)}
                  />
                </Grid>
              </Grid>
            </CardContent>
          </Card>
        );
      case 6:
        return (
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>Review & Generate</Typography>
              <Accordion sx={{ mb: 2 }}>
                <AccordionSummary expandIcon={<ExpandMoreIcon />}>
                  <Typography variant="subtitle1">Basic Info</Typography>
                </AccordionSummary>
                <AccordionDetails>
                  <Box sx={{ display: 'flex', gap: 4 }}>
                    <Box><strong>Name:</strong> {formData.name}</Box>
                    <Box><strong>Description:</strong> {formData.description}</Box>
                  </Box>
                </AccordionDetails>
              </Accordion>
              <Accordion sx={{ mb: 2 }}>
                <AccordionSummary expandIcon={<ExpandMoreIcon />}>
                  <Typography variant="subtitle1">Source Database</Typography>
                </AccordionSummary>
                <AccordionDetails>
                  <Box><strong>Type:</strong> {formData.source.type}</Box>
                  <Box><strong>Host:</strong> {formData.source.host}:{formData.source.port}</Box>
                  <Box><strong>Database:</strong> {formData.source.databaseName}</Box>
                  <Box><strong>User:</strong> {formData.source.username}</Box>
                </AccordionDetails>
              </Accordion>
              <Accordion sx={{ mb: 2 }}>
                <AccordionSummary expandIcon={<ExpandMoreIcon />}>
                  <Typography variant="subtitle1">Target</Typography>
                </AccordionSummary>
                <AccordionDetails>
                  <Box><strong>Type:</strong> {formData.target.type || 'Kafka (streaming only)'}</Box>
                  {formData.target.host && <Box><strong>Host:</strong> {formData.target.host}:{formData.target.port}</Box>}
                </AccordionDetails>
              </Accordion>
              <Accordion sx={{ mb: 2 }}>
                <AccordionSummary expandIcon={<ExpandMoreIcon />}>
                  <Typography variant="subtitle1">Kafka & Schema Registry</Typography>
                </AccordionSummary>
                <AccordionDetails>
                  <Box><strong>Bootstrap Servers:</strong> {formData.kafka.bootstrapServers}</Box>
                  <Box><strong>Topic Prefix:</strong> {formData.kafka.topicPrefix || '(none)'}</Box>
                  <Box><strong>Schema Registry:</strong> {formData.schemaRegistry.url}</Box>
                </AccordionDetails>
              </Accordion>
              <Accordion>
                <AccordionSummary expandIcon={<ExpandMoreIcon />}>
                  <Typography variant="subtitle1">Deployment</Typography>
                </AccordionSummary>
                <AccordionDetails>
                  <Box><strong>Type:</strong> {formData.deployment.type}</Box>
                  <Box><strong>Namespace:</strong> {formData.deployment.namespace}</Box>
                  <Box><strong>Replicas:</strong> {formData.deployment.replicas}</Box>
                </AccordionDetails>
              </Accordion>
            </CardContent>
          </Card>
        );
      default:
        return null;
    }
  };

  return (
    <Box sx={{ maxWidth: '900px', mx: 'auto' }}>
      <Box sx={{ mb: 4 }}>
        <Typography variant="h4" sx={{ fontWeight: 700, mb: 1 }}>
          {isEditing ? 'Edit Pipeline' : 'Create New Pipeline'}
        </Typography>
        <Typography variant="body1" color="text.secondary">
          Configure your Debezium CDC pipeline step by step
        </Typography>
      </Box>

      {error && (
        <Alert severity="error" sx={{ mb: 3 }}>
          <AlertTitle>Error</AlertTitle>
          {error}
        </Alert>
      )}

      {success && (
        <Alert severity="success" sx={{ mb: 3 }}>
          <AlertTitle>Success</AlertTitle>
          {success}
        </Alert>
      )}

      <Stepper activeStep={activeStep} alternativeLabel sx={{ mb: 4 }}>
        {steps.map((step, index) => (
          <Step key={step.label}>
            <StepLabel
              StepIconComponent={(props) => (
                <Box
                  sx={{
                    width: 32,
                    height: 32,
                    borderRadius: '50%',
                    backgroundColor: index < activeStep ? 'primary.main' : index === activeStep ? 'primary.main' : 'grey[300]',
                    color: index <= activeStep ? 'white' : 'grey[600]',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    fontWeight: 600,
                    fontSize: '0.875rem',
                  }}
                >
                  {index < activeStep ? <CheckCircleIcon fontSize="small" /> : index + 1}
                </Box>
              )}
            >
              <Typography variant="caption">{step.label}</Typography>
            </StepLabel>
          </Step>
        ))}
      </Stepper>

      <Box sx={{ mb: 4 }}>
        {renderStepContent()}
      </Box>

      <Box sx={{ display: 'flex', justifyContent: 'space-between', pt: 2, borderTop: 1, borderColor: 'divider' }}>
        <Button
          variant="outlined"
          onClick={handleBack}
          disabled={activeStep === 0}
          startIcon={<EditIcon />}
        >
          Back
        </Button>
        <Box sx={{ display: 'flex', gap: 2 }}>
          {activeStep < steps.length - 1 ? (
            <Button
              variant="contained"
              onClick={handleNext}
              disabled={!validateStep() || loading}
              endIcon={<PlayArrowIcon />}
            >
              Next
            </Button>
          ) : (
            <>
              <Button
                variant="outlined"
                onClick={() => handleSubmit(false)}
                disabled={loading}
                startIcon={<SaveIcon />}
              >
                Generate Config
              </Button>
              <Button
                variant="contained"
                onClick={() => handleSubmit(true)}
                disabled={loading}
                startIcon={<CloudUploadIcon />}
              >
                Generate & Deploy
              </Button>
            </>
          )}
        </Box>
      </Box>
    </Box>
  );
}

export default PipelineBuilder;